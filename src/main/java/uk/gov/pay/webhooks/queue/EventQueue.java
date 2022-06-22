package uk.gov.pay.webhooks.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.queue.sqs.QueueException;
import uk.gov.pay.webhooks.queue.sqs.QueueMessage;
import uk.gov.pay.webhooks.queue.sqs.SqsQueueService;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class EventQueue {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventQueue.class);

    private static final String EVENT_MESSAGE_ATTRIBUTE_NAME = "All";

    private final SqsQueueService sqsQueueService;
    private final String eventQueueUrl;
    private final ObjectMapper objectMapper;
    private final int retryDelayInSeconds;

    @Inject
    public EventQueue(SqsQueueService sqsQueueService, WebhooksConfig configuration, ObjectMapper objectMapper) {
        this.sqsQueueService = sqsQueueService;
        this.eventQueueUrl = configuration.getSqsConfig().getEventQueueUrl();
        this.objectMapper = objectMapper;
        this.retryDelayInSeconds = 5;
    }

    public List<EventMessage> retrieveEvents() throws QueueException {
        List<QueueMessage> queueMessages = sqsQueueService.receiveMessages(this.eventQueueUrl, EVENT_MESSAGE_ATTRIBUTE_NAME);

        return queueMessages
                .stream()
                .map(this::getMessage)
                .filter(Objects::nonNull)
                .toList();
    }

    public void markMessageAsProcessed(EventMessage message) throws QueueException {
        sqsQueueService.deleteMessage(this.eventQueueUrl, message.queueMessage().receiptHandle());
    }

    public void scheduleMessageForRetry(EventMessage message) throws QueueException {
        sqsQueueService.deferMessage(this.eventQueueUrl, message.queueMessage().receiptHandle(), retryDelayInSeconds);
    }

    private EventMessage getMessage(QueueMessage queueMessage) {
        try {
            // @TODO(sfount) additional SNS message layer added to unpack the contents from SNS
            //               this should be sense checked with the publisher (currently Ledger)
            //               to see if this should be unpacked here or if something can be optimised with the publish
            SNSMessageDto snsMessageDto = objectMapper.readValue(queueMessage.messageBody(), SNSMessageDto.class);
            EventMessageDto eventMessageDto = objectMapper.readValue(snsMessageDto.Message(), EventMessageDto.class);
            if (eventMessageDto.live() == null || eventMessageDto.serviceId() == null) {
                LOGGER.warn("Unable to process events without `service_id` or `live` properties");
                return null;
            }
            return EventMessage.of(eventMessageDto, queueMessage);
        } catch (IOException e) {
            LOGGER.warn(
                    "There was an exception parsing message [messageId={}] into an [{}] {}",
                    queueMessage.messageId(),
                    EventMessage.class, e.getMessage());

            return null;
        }
    }

}

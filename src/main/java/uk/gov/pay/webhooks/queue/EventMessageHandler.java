package uk.gov.pay.webhooks.queue;

import com.google.inject.Inject;
import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.message.WebhookMessageService;
import uk.gov.pay.webhooks.queue.sqs.QueueException;

import static net.logstash.logback.argument.StructuredArguments.kv;

public class EventMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);

    private final EventQueue eventQueue;
    private final WebhookMessageService webhookMessageService;

    @Inject
    public EventMessageHandler(EventQueue eventQueue, WebhookMessageService webhookMessageService) {
        this.eventQueue = eventQueue;
        this.webhookMessageService = webhookMessageService;
    }

    public void handle() throws QueueException {
        for (EventMessage message : eventQueue.retrieveEvents()) {
            try {
                processSingleMessage(message);
            } catch (Exception e) {
                LOGGER.error("Error during handling the event message",
                        kv("sqs_message_id", message.queueMessage().messageId()),
                        kv("resource_external_id", message.eventMessageDto().resourceExternalId()),
                        kv("error", e.getMessage()));
            }
        }
    }

    @UnitOfWork
    protected void processSingleMessage(EventMessage message) throws QueueException {
        try {
            webhookMessageService.handleInternalEvent(message.toInternalEvent());
            eventQueue.markMessageAsProcessed(message);
        } catch (Exception e) {
            LOGGER.warn("Event message with ID %s scheduled for retry: %s".formatted(message.queueMessage().messageId(), e.getMessage()));
            eventQueue.scheduleMessageForRetry(message);
        }
    }
}

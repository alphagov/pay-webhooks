package uk.gov.pay.webhooks.queue;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.message.WebhookMessageService;
import uk.gov.pay.webhooks.queue.sqs.QueueException;

import java.io.IOException;

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
                LOGGER.warn("Error during handling the event message with ID %s: ".formatted(message.queueMessage().messageId(), e.getMessage()));
            }
        }
    }

    private void processSingleMessage(EventMessage message) throws QueueException {
        try {
            webhookMessageService.handleInternalEvent(message.toInternalEvent());
            eventQueue.markMessageAsProcessed(message);
        } catch (IOException | InterruptedException e) {
            eventQueue.scheduleMessageForRetry(message);
            LOGGER.warn("Event message with ID %s scheduled for retry".formatted(message.queueMessage().messageId()));
        }
    }

}

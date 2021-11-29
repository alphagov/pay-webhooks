package uk.gov.pay.webhooks.queue;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.message.WebhookMessageService;

import java.io.IOException;
import java.util.List;


public class EventMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);

    private final EventQueue eventQueue;

    private final WebhookMessageService webhookMessageService;
    @Inject
    public EventMessageHandler(EventQueue eventQueue,
                               WebhookMessageService webhookMessageService) {
        this.eventQueue = eventQueue;
        this.webhookMessageService = webhookMessageService;
    }

    
    public void handle() throws QueueException {
        List<EventMessage> eventMessages = eventQueue.retrieveEvents();

        for (EventMessage message : eventMessages) {
            try {
                processSingleMessage(message);
            } catch (Exception e) {
                LOGGER.warn("Error during handling the event message %s".formatted(e.getMessage()));
            }
        }
    }

    private void processSingleMessage(EventMessage message) throws QueueException {
        try {
            webhookMessageService.handleInternalEvent(message.getEvent());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

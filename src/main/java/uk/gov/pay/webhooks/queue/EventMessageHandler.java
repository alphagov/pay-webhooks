package uk.gov.pay.webhooks.queue;

import com.google.inject.Inject;
import io.dropwizard.hibernate.UnitOfWork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.webhookevent.WebhookEventService;

import java.util.List;


public class EventMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);

    private final EventQueue eventQueue;

    private final WebhookEventService webhookEventService;
    @Inject
    public EventMessageHandler(EventQueue eventQueue,
                               WebhookEventService webhookEventService) {
        this.eventQueue = eventQueue;
        this.webhookEventService = webhookEventService;
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
        webhookEventService.handleInternalMessage(message.getEvent());
    }
}

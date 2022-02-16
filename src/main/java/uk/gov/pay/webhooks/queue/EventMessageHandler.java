package uk.gov.pay.webhooks.queue;

import com.google.inject.Inject;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.message.WebhookMessageService;
import uk.gov.pay.webhooks.queue.sqs.QueueException;

import static net.logstash.logback.argument.StructuredArguments.kv;

public class EventMessageHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventMessageHandler.class);

    private final EventQueue eventQueue;
    private final WebhookMessageService webhookMessageService;
    private final SessionFactory sessionFactory;

    @Inject
    public EventMessageHandler(EventQueue eventQueue, WebhookMessageService webhookMessageService, SessionFactory sessionFactory) {
        this.eventQueue = eventQueue;
        this.webhookMessageService = webhookMessageService;
        this.sessionFactory = sessionFactory;
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

    private void processSingleMessage(EventMessage message) throws QueueException {
        var session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);
        var transaction = session.beginTransaction();

        try {
            webhookMessageService.handleInternalEvent(message.toInternalEvent());
            transaction.commit();
            eventQueue.markMessageAsProcessed(message);
        } catch (Exception e) {
            LOGGER.warn("Event message with ID %s scheduled for retry: %s".formatted(message.queueMessage().messageId(), e.getMessage()));
            transaction.rollback();
            eventQueue.scheduleMessageForRetry(message);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

}

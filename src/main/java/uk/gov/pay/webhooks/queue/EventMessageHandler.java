package uk.gov.pay.webhooks.queue;

import com.google.inject.Inject;
import io.dropwizard.hibernate.UnitOfWork;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.yaml.snakeyaml.error.Mark;
import uk.gov.pay.webhooks.message.WebhookMessageService;
import uk.gov.pay.webhooks.queue.sqs.QueueException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static uk.gov.pay.webhooks.app.WebhooksKeys.ERROR;
import static uk.gov.pay.webhooks.app.WebhooksKeys.ERROR_MESSAGE;
import static uk.gov.pay.webhooks.app.WebhooksKeys.JOB_BATCH_ID;
import static uk.gov.pay.webhooks.app.WebhooksKeys.RESOURCE_IS_LIVE;
import static uk.gov.pay.webhooks.app.WebhooksKeys.SQS_MESSAGE_ID;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_RESOURCE_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.MDC_REQUEST_ID_KEY;
import static uk.gov.service.payments.logging.LoggingKeys.SERVICE_EXTERNAL_ID;

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
                MDC.put(MDC_REQUEST_ID_KEY, UUID.randomUUID().toString());
                MDC.put(SQS_MESSAGE_ID, message.queueMessage().messageId());
                processSingleMessage(message);
            } catch (Exception e) {
                LOGGER.error(
                        Markers.append(ERROR_MESSAGE, e.getMessage()),
                        "Error during handling event message"
                );
            } finally {
                List.of(SQS_MESSAGE_ID, MDC_REQUEST_ID_KEY).forEach(MDC::remove);
            }
        }
    }

    @UnitOfWork
    protected void processSingleMessage(EventMessage message) throws QueueException {
        try {
            var event = message.toInternalEvent();
            MDC.put(SERVICE_EXTERNAL_ID, event.serviceId());
            MDC.put(RESOURCE_IS_LIVE, Optional.ofNullable(event.live()).map(String::valueOf).orElse(null));
            MDC.put(WEBHOOK_MESSAGE_RESOURCE_EXTERNAL_ID, event.resourceExternalId());

            webhookMessageService.handleInternalEvent(event);
            eventQueue.markMessageAsProcessed(message);
        } catch (Exception e) {
            LOGGER.warn(
                    Markers.append(ERROR_MESSAGE, e.getMessage())
                            .and(Markers.append(ERROR, e)),
                    "Event message scheduled for retry"
            );
            eventQueue.scheduleMessageForRetry(message);
        } finally {
            List.of(SERVICE_EXTERNAL_ID, RESOURCE_IS_LIVE, WEBHOOK_MESSAGE_RESOURCE_EXTERNAL_ID).forEach(MDC::remove);
        }
    }
}

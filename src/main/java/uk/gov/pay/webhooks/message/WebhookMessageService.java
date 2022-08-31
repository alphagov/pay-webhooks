package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.logstash.logback.marker.Markers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeDao;
import uk.gov.pay.webhooks.ledger.LedgerService;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;
import uk.gov.pay.webhooks.message.apirepresentation.PaymentApiRepresentation;
import uk.gov.pay.webhooks.message.apirepresentation.RefundApiRepresentation;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.queue.InternalEvent;
import uk.gov.pay.webhooks.util.IdGenerator;
import uk.gov.pay.webhooks.webhook.WebhookService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.inject.Inject;
import java.io.IOException;
import java.time.InstantSource;
import java.util.Optional;

import static uk.gov.pay.webhooks.app.WebhooksKeys.STATE_TRANSITION_TO_STATE;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_EXTERNAL_ID;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_EVENT_INTERNAL_TYPE;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_EVENT_TYPE;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_EXTERNAL_ID;

public class WebhookMessageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookMessageService.class);

    private final WebhookService webhookService;
    private final LedgerService ledgerService;
    private final EventTypeDao eventTypeDao;
    private final InstantSource instantSource;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;
    private final WebhookMessageDao webhookMessageDao;
    private final WebhookDeliveryQueueDao webhookDeliveryQueueDao;

    @Inject
    public WebhookMessageService(WebhookService webhookService,
                                 LedgerService ledgerService,
                                 EventTypeDao eventTypeDao,
                                 InstantSource instantSource,
                                 IdGenerator idGenerator,
                                 ObjectMapper objectMapper,
                                 WebhookMessageDao webhookMessageDao,
                                 WebhookDeliveryQueueDao webhookDeliveryQueueDao) {
        this.webhookService = webhookService;
        this.ledgerService = ledgerService;
        this.eventTypeDao = eventTypeDao;
        this.instantSource = instantSource;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
        this.webhookMessageDao = webhookMessageDao;
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
    }
    
    public void handleInternalEvent(InternalEvent event) throws IOException, InterruptedException {
        if (event.live() == null || event.serviceId() == null) {
            LOGGER.info("Ignoring event without `service_id` or `live` properties");
            return;
        }

        var subscribedWebhooks = webhookService.getWebhooksSubscribedToEvent(event);

        if (!subscribedWebhooks.isEmpty()) {
            var resourceExternalId = getResourceIdForEvent(event);
            LOGGER.info(
                    Markers.append(WEBHOOK_MESSAGE_EVENT_INTERNAL_TYPE, event.eventType()),
                    "Got subscribed webhook endpoints"
            );
            LedgerTransaction ledgerTransaction = ledgerService.getTransaction(resourceExternalId).orElseThrow(IllegalArgumentException::new);

            for (WebhookEntity webhook : subscribedWebhooks) {
               buildWebhookMessage(webhook, event, ledgerTransaction)
                       .ifPresent(message -> {
                           var entity = webhookMessageDao.create(message);
                           webhookDeliveryQueueDao.enqueueFrom(entity, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant());
                           LOGGER.info(
                                   Markers.append(WEBHOOK_MESSAGE_EXTERNAL_ID, entity.getExternalId())
                                           .and(Markers.append(WEBHOOK_EXTERNAL_ID, entity.getWebhookEntity().getExternalId()))
                                           .and(Markers.append(STATE_TRANSITION_TO_STATE, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING))
                                           .and(Markers.append(WEBHOOK_MESSAGE_EVENT_TYPE, entity.getEventType().getName().getName())),
                                   "Persisted and queued webhook message to send"
                           );
                       });
            }
        }
    }

    private Optional<WebhookMessageEntity> buildWebhookMessage(WebhookEntity webhook, InternalEvent event, LedgerTransaction ledgerTransaction) {
        return switch (event.resourceType()) {
            case "payment" -> {
                JsonNode resource = objectMapper.valueToTree(PaymentApiRepresentation.of(ledgerTransaction));
                yield Optional.of(buildWebhookMessageEntity(webhook, event, resource));
            }
            case "refund" -> {
                JsonNode resource = objectMapper.valueToTree(RefundApiRepresentation.of(ledgerTransaction));
                yield Optional.of(buildWebhookMessageEntity(webhook, event, resource));
            }
            default -> {
                LOGGER.info("Ignoring unsupported resource type %s".formatted(event.resourceType()));
                yield Optional.empty();
            }
        };
    }

    private WebhookMessageEntity buildWebhookMessageEntity(WebhookEntity webhook, InternalEvent event, JsonNode resource) {
        var webhookMessageEntity = new WebhookMessageEntity();
        webhookMessageEntity.setExternalId(idGenerator.newExternalId());
        webhookMessageEntity.setCreatedDate(instantSource.instant());
        webhookMessageEntity.setWebhookEntity(webhook);
        webhookMessageEntity.setEventDate(event.timestamp());
        webhookMessageEntity.setEventType(eventTypeDao.findByName(EventMapper.getWebhookEventNameFor(event.eventType())).orElseThrow(IllegalArgumentException::new));
        webhookMessageEntity.setResource(resource);
        webhookMessageEntity.setResourceExternalId(event.resourceExternalId());
        webhookMessageEntity.setResourceType(event.resourceType());
        return webhookMessageEntity;
    }

    private String getResourceIdForEvent(InternalEvent event) {
        var isChildEvent = EventMapper.isChildEvent(EventMapper.getWebhookEventNameFor(event.eventType()));
        return isChildEvent ? event.parentResourceExternalId() : event.resourceExternalId();
    }
}

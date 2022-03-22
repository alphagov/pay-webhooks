package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        var subscribedWebhooks = webhookService.getWebhooksSubscribedToEvent(event);

        if (!subscribedWebhooks.isEmpty()) {
            var resourceExternalId = getResourceIdForEvent(event); 
            LedgerTransaction ledgerTransaction = ledgerService.getTransaction(resourceExternalId).orElseThrow(IllegalArgumentException::new);
            subscribedWebhooks
                    .stream()
                    .map(webhook -> buildWebhookMessage(webhook, event, ledgerTransaction))
                    .flatMap(Optional::stream)
                    .map(webhookMessageDao::create)
                    .forEach(webhookMessageEntity -> webhookDeliveryQueueDao.enqueueFrom(webhookMessageEntity, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant()));
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

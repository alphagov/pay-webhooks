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
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.publicapi.model.Address;
import uk.gov.pay.webhooks.publicapi.model.AuthorisationSummary;
import uk.gov.pay.webhooks.publicapi.model.CardDetails;
import uk.gov.pay.webhooks.publicapi.model.ExternalChargeState;
import uk.gov.pay.webhooks.publicapi.model.PaymentForSearchResult;
import uk.gov.pay.webhooks.publicapi.model.PaymentSettlementSummary;
import uk.gov.pay.webhooks.publicapi.model.PaymentState;
import uk.gov.pay.webhooks.publicapi.model.RefundSummary;
import uk.gov.pay.webhooks.publicapi.model.ThreeDSecure;
import uk.gov.pay.webhooks.queue.InternalEvent;
import uk.gov.pay.webhooks.util.IdGenerator;
import uk.gov.pay.webhooks.webhook.WebhookService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;

import javax.inject.Inject;
import java.io.IOException;
import java.time.InstantSource;
import java.util.Date;
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
                    .map(webhookMessageDao::create)
                    .forEach(webhookMessageEntity -> webhookDeliveryQueueDao.enqueueFrom(webhookMessageEntity, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, Date.from(instantSource.instant())));
        }
    }

    private WebhookMessageEntity buildWebhookMessage(WebhookEntity webhook, InternalEvent event, LedgerTransaction ledgerTransaction) {
        JsonNode resource = objectMapper.valueToTree(paymentForSearchResultFrom(ledgerTransaction));
        
        var webhookMessageEntity = new WebhookMessageEntity();
        webhookMessageEntity.setExternalId(idGenerator.newExternalId());
        webhookMessageEntity.setCreatedDate(Date.from(instantSource.instant()));
        webhookMessageEntity.setWebhookEntity(webhook);
        webhookMessageEntity.setEventDate(Date.from(event.eventDate()));
        webhookMessageEntity.setEventType(eventTypeDao.findByName(EventMapper.getWebhookEventNameFor(event.eventType())).orElseThrow(IllegalArgumentException::new));
        webhookMessageEntity.setResource(objectMapper.valueToTree(resource)); // will probably need some more transformation
        webhookMessageEntity.setResourceExternalId(event.resourceExternalId());
        webhookMessageEntity.setResourceType(event.resourceType());
        return webhookMessageEntity;
    }

    private String getResourceIdForEvent(InternalEvent event) {
        var isChildEvent = EventMapper.isChildEvent(EventMapper.getWebhookEventNameFor(event.eventType()));
        return isChildEvent ? event.parentResourceExternalId() : event.resourceExternalId();
    }
}

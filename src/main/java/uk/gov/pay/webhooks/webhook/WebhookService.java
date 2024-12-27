package uk.gov.pay.webhooks.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.app.WebhookMessageDeletionConfig;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeDao;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.EventMapper;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.message.resource.WebhookDeliveryQueueResponse;
import uk.gov.pay.webhooks.message.resource.WebhookMessageResponse;
import uk.gov.pay.webhooks.message.resource.WebhookMessageSearchResponse;
import uk.gov.pay.webhooks.queue.InternalEvent;
import uk.gov.pay.webhooks.util.IdGenerator;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;
import uk.gov.pay.webhooks.webhook.resource.CreateWebhookRequest;
import uk.gov.pay.webhooks.webhook.resource.WebhookMessageSearchParams;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchOp;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import java.time.InstantSource;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_CALLBACK_URL;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_DESCRIPTION;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_STATUS;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_SUBSCRIPTIONS;

public class WebhookService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookService.class);
    
    private final WebhookDao webhookDao;
    private final WebhookMessageDao webhookMessageDao;
    private final WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private final EventTypeDao eventTypeDao;
    private final InstantSource instantSource;
    private final IdGenerator idGenerator;
    private final WebhookMessageDeletionConfig webhookMessageDeletionConfig;

    @Inject
    public WebhookService(WebhookDao webhookDao, EventTypeDao eventTypeDao, InstantSource instantSource,
                          IdGenerator idGenerator, WebhookMessageDao webhookMessageDao,
                          WebhookDeliveryQueueDao webhookDeliveryQueueDao, WebhookMessageDeletionConfig webhookMessageDeletionConfig) {
        this.webhookDao = webhookDao;
        this.eventTypeDao = eventTypeDao;
        this.instantSource = instantSource;
        this.idGenerator = idGenerator;
        this.webhookMessageDao = webhookMessageDao;
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
        this.webhookMessageDeletionConfig = webhookMessageDeletionConfig;
    }

    public WebhookEntity createWebhook(CreateWebhookRequest createWebhookRequest) {
        var webhookEntity = WebhookEntity.from(createWebhookRequest, idGenerator.newExternalId(), instantSource.instant(), idGenerator.newWebhookSigningKey(createWebhookRequest.live()));
        if (createWebhookRequest.subscriptions() != null) {
            List<EventTypeEntity> subscribedEventTypes = createWebhookRequest.subscriptions().stream()
                    .map(eventTypeDao::findByName)
                    .flatMap(Optional::stream)
                    .toList();
            webhookEntity.addSubscriptions(subscribedEventTypes);
        }
        webhookDao.create(webhookEntity);
        
        return webhookEntity;
    }

    public Optional<WebhookEntity> findByExternalIdAndServiceId(String externalId, String serviceId) {
        return webhookDao.findByExternalIdAndServiceId(externalId, serviceId);
    }
    
    public Optional<WebhookEntity> findByExternalIdAndGatewayAccountId(String externalId, String gatewayAccountId) {
        return webhookDao.findByExternalIdAndGatewayAccountId(externalId, gatewayAccountId);
    }

    public Optional<WebhookEntity> findByExternalId(String externalId) {
        return webhookDao.findByExternalId(externalId);
    }

    public List<WebhookEntity> list(boolean live, String serviceId) {
        return webhookDao.list(live, serviceId);
    }

    public List<WebhookEntity> list(boolean live) {
        return webhookDao.list(live);
    }
    
    public List<WebhookEntity> listByGatewayAccountId(String gatewayAccountId) {
        return webhookDao.listByGatewayAccountId(gatewayAccountId);
    }

    public WebhookMessageSearchResponse listMessages(String webhookId, WebhookMessageSearchParams queryParams) {
        var webhook = webhookDao.findByExternalId(webhookId).orElseThrow(NotFoundException::new);
        var totalMessagesCount = webhookMessageDao.getTotalMessagesCount(webhook, queryParams);
        var messages = webhookMessageDao.list(webhook, queryParams)
                .stream()
                .map(WebhookMessageResponse::from)
                .toList();
        return new WebhookMessageSearchResponse(messages.size(), queryParams.getPage(), totalMessagesCount, messages);
    }

    public Optional<WebhookMessageResponse> getMessage(String webhookId, String messageId) {
        return webhookDao.findByExternalId(webhookId)
                .flatMap(webhookEntity -> webhookMessageDao.get(webhookEntity, messageId))
                .map(WebhookMessageResponse::from);
    }

    public List<WebhookDeliveryQueueResponse> listMessageAttempts(String webhookId, String messageId) {
        return webhookDeliveryQueueDao.list(webhookId, messageId)
                .stream()
                .map(WebhookDeliveryQueueResponse::from)
                .toList();
    }

    public Optional<WebhookEntity> regenerateSigningKey(String externalId, String gatewayAccountId) {
         return webhookDao.findByExternalIdAndGatewayAccountId(externalId, gatewayAccountId).map(webhookEntity -> { 
          webhookEntity.setSigningKey(idGenerator.newWebhookSigningKey(webhookEntity.isLive()));
             return webhookEntity;
         });
    } 
    
    public WebhookEntity update(String externalId, String serviceId, List<JsonPatchRequest> patchRequests) {
        return webhookDao.findByExternalIdAndServiceId(externalId, serviceId).map(webhookEntity -> {
            patchRequests.forEach(patchRequest -> {
                if (JsonPatchOp.REPLACE == patchRequest.getOp()) {
                    switch (patchRequest.getPath()) {
                        case FIELD_DESCRIPTION -> webhookEntity.setDescription(patchRequest.valueAsString());
                        case FIELD_CALLBACK_URL -> webhookEntity.setCallbackUrl(patchRequest.valueAsString());
                        case FIELD_STATUS -> webhookEntity.setStatus(WebhookStatus.of(patchRequest.valueAsString()));
                        case FIELD_SUBSCRIPTIONS -> webhookEntity.replaceSubscriptions(patchRequest.valueAsListOfString()
                                .stream()
                                .map(EventTypeName::of)
                                .map(eventTypeDao::findByName)
                                .flatMap(Optional::stream)
                                .toList());
                        default -> throw new BadRequestException("Unexpected path for patch operation: " + patchRequest.getPath());
                    }
                }
            });
            return webhookEntity;
        }).orElseThrow(NotFoundException::new);
    }

    public List<WebhookEntity> getWebhooksSubscribedToEvent(InternalEvent event) {
        return webhookDao.listByGatewayAccountId(event.gatewayAccountId())
                .stream()
                .filter(webhook -> webhookHasSubscriptionForEvent(webhook, event))
                .toList();
    }

    private boolean webhookHasSubscriptionForEvent(WebhookEntity webhook, InternalEvent event) {
        return webhook.getSubscriptions().stream()
                .map(EventTypeEntity::getName)
                .map(EventMapper::getInternalEventNamesFor)
                .anyMatch(internalEventNames -> internalEventNames.contains(event.eventType()));
    }

    public void deleteWebhookMessages() {
        int maxAgeOfMessages = webhookMessageDeletionConfig.getMaxAgeOfMessages();
        int maxNumOfMessagesToExpire = webhookMessageDeletionConfig.getMaxNumOfMessagesToExpire();

        int numberOfWebhookMessagesDeleted = webhookMessageDao.deleteMessages(maxAgeOfMessages, maxNumOfMessagesToExpire);
        LOGGER.info(format("%s webhook messages were deleted.", numberOfWebhookMessagesDeleted));
    }
}

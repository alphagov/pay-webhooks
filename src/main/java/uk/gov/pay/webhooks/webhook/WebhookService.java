package uk.gov.pay.webhooks.webhook;

import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeDao;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.EventMapper;
import uk.gov.pay.webhooks.queue.InternalEvent;
import uk.gov.pay.webhooks.util.ExternalIdGenerator;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;
import uk.gov.pay.webhooks.webhook.resource.CreateWebhookRequest;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchOp;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.time.InstantSource;
import java.util.List;
import java.util.Optional;

import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_CALLBACK_URL;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_DESCRIPTION;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_STATUS;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_SUBSCRIPTIONS;

public class WebhookService {

    private final WebhookDao webhookDao;
    private final EventTypeDao eventTypeDao;
    private final InstantSource instantSource;
    private final ExternalIdGenerator externalIdGenerator;

    @Inject
    public WebhookService(WebhookDao webhookDao, EventTypeDao eventTypeDao, InstantSource instantSource, ExternalIdGenerator externalIdGenerator) {
        this.webhookDao = webhookDao;
        this.eventTypeDao = eventTypeDao;
        this.instantSource = instantSource;
        this.externalIdGenerator = externalIdGenerator;
    }

    public WebhookEntity createWebhook(CreateWebhookRequest createWebhookRequest) {
        var webhookEntity = WebhookEntity.from(createWebhookRequest, externalIdGenerator.newExternalId(), instantSource.instant());
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

    public Optional<WebhookEntity> findByExternalId(String externalId, String serviceId) {
        return webhookDao.findByExternalId(externalId, serviceId);
    }    
    
    public List<WebhookEntity> list(boolean live, String serviceId) {
        return webhookDao.list(live, serviceId);
    }      
    
    public List<WebhookEntity> list(boolean live) {
        return webhookDao.list(live);
    }
    
    public WebhookEntity update(String externalId, String serviceId, List<JsonPatchRequest> patchRequests) {
        return webhookDao.findByExternalId(externalId, serviceId).map(webhookEntity -> {
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
        return list(event.live(), event.serviceId())
                .stream()
                .filter(webhook -> webhookHasSubscriptionForEvent(webhook, event))
                .toList();
    }

    private boolean webhookHasSubscriptionForEvent(WebhookEntity webhook, InternalEvent event) {
        return webhook.getSubscriptions().stream()
                .map(EventTypeEntity::getName)
                .map(EventMapper::getInternalEventNameFor)
                .flatMap(Optional::stream)
                .anyMatch(internalEventName -> internalEventName.equals(event.eventType()));
    }
}

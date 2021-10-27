package uk.gov.pay.webhooks.webhook;

import uk.gov.pay.webhooks.eventtype.dao.EventTypeDao;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;
import uk.gov.pay.webhooks.webhook.resource.CreateWebhookRequest;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchOp;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.util.List;
import java.util.Optional;

import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_CALLBACK_URL;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_DESCRIPTION;
import static uk.gov.pay.webhooks.webhook.resource.WebhookResponse.FIELD_STATUS;

public class WebhookService {
    WebhookDao webhookDao;
    
    EventTypeDao eventTypeDao;

    @Inject
    public WebhookService(WebhookDao webhookDao, EventTypeDao eventTypeDao) {
        this.webhookDao = webhookDao;
        this.eventTypeDao = eventTypeDao;
    }

    public WebhookEntity createWebhook(CreateWebhookRequest createWebhookRequest) {
        var webhookEntity = WebhookEntity.from(createWebhookRequest);
        if (createWebhookRequest.subscriptions() != null) {
            List<EventTypeEntity> subscribedEventTypes = createWebhookRequest.subscriptions().stream()
                    .map(eventTypeName -> eventTypeDao.findByName(eventTypeName))
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
                        default -> throw new BadRequestException("Unexpected path for patch operation: " + patchRequest.getPath());
                    }
                }
            });
            return webhookEntity;
        }).orElseThrow(NotFoundException::new);
    }
    
}

package uk.gov.pay.webhooks.webhook;

import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeDao;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.resource.CreateWebhookRequest;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

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
}

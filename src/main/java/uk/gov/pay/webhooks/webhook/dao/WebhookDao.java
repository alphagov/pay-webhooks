package uk.gov.pay.webhooks.webhook.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class WebhookDao extends AbstractDAO<WebhookEntity> {
    
    @Inject
    public WebhookDao(SessionFactory factory) {
        super(factory);
    }

    public WebhookEntity create(WebhookEntity webhook) {
        persist(webhook);
        return webhook;
    }

    public Optional<WebhookEntity> findByExternalIdAndServiceId(String webhookExternalId, String serviceId) {
            return namedTypedQuery(WebhookEntity.GET_BY_EXTERNAL_ID_AND_SERVICE_ID)
                    .setParameter("externalId", webhookExternalId)
                    .setParameter("serviceId", serviceId)
                    .getResultList()
                    .stream()
                    .findFirst();
    }

    public Optional<WebhookEntity> findByExternalId(String webhookExternalId) {
        return namedTypedQuery(WebhookEntity.GET_BY_EXTERNAL_ID)
                .setParameter("externalId", webhookExternalId)
                .getResultList()
                .stream()
                .findFirst();
    }

    public List<WebhookEntity> list(boolean live, String serviceId) {
        return namedTypedQuery(WebhookEntity.LIST_BY_LIVE_AND_SERVICE_ID)
        .setParameter("serviceId", serviceId)
        .setParameter("live", live)
        .getResultList();
    }

    public List<WebhookEntity> list(boolean live) {
        return  namedTypedQuery(WebhookEntity.LIST_BY_LIVE)
                .setParameter("live", live)
                .getResultList();
    }
}

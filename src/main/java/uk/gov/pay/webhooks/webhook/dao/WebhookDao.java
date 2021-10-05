package uk.gov.pay.webhooks.webhook.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.inject.Inject;
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

    public Optional<WebhookEntity> findByExternalId(String webhookExternalId) {
        return Optional.ofNullable(namedTypedQuery(WebhookEntity.GET_BY_EXTERNAL_ID)
                .setParameter("externalId", webhookExternalId)
                .uniqueResult());
    }

    public Optional<WebhookEntity> findByServiceId(String serviceId) {
        return Optional.ofNullable(namedTypedQuery(WebhookEntity.GET_BY_SERVICE_ID)
                .setParameter("serviceId", serviceId)
                .uniqueResult());
    }
}

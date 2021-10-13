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

    public Optional<WebhookEntity> findByExternalId(String webhookExternalId, String serviceId) {
        return Optional.ofNullable(namedTypedQuery(WebhookEntity.GET_BY_EXTERNAL_ID_AND_SERVICE_ID)
                .setParameter("externalId", webhookExternalId)
                .setParameter("serviceId", serviceId)
                .getSingleResult());
    }
}

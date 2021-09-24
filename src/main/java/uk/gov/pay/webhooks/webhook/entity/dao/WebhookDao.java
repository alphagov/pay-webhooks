package uk.gov.pay.webhooks.webhook.entity.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.webhook.entity.Webhook;

import javax.inject.Inject;
import java.util.Optional;

public class WebhookDao extends AbstractDAO<Webhook> {
    
    @Inject
    public WebhookDao(SessionFactory factory) {
        super(factory);
    }

    public Webhook create(Webhook webhook) {
        persist(webhook);
        return webhook;
    }

    public Optional<Webhook> findByExternalId(String webhookExternalId) {
        return Optional.ofNullable(namedTypedQuery(Webhook.GET_BY_EXTERNAL_ID)
                .setParameter("externalId", webhookExternalId)
                .getSingleResult());
    }
}

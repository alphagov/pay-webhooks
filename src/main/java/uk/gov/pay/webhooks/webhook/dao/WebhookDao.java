package uk.gov.pay.webhooks.webhook.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
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

    public void deleteByExternalId(String externalId) {
        Optional<WebhookEntity> webhookEntity = findByExternalId(externalId);
        var we = webhookEntity.orElseThrow(NotFoundException::new);
        currentSession().delete(we);
    }

    public Optional<WebhookEntity> findByExternalId(String webhookExternalId) {
        return Optional.ofNullable(namedTypedQuery(WebhookEntity.GET_BY_EXTERNAL_ID)
                .setParameter("externalId", webhookExternalId)
                .getSingleResult());
    }
}

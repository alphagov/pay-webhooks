package uk.gov.pay.webhooks.webhook.entity.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.webhook.entity.WebhookEntity;

import javax.inject.Inject;

public class WebhookDao extends AbstractDAO<WebhookEntity> {
    
    @Inject
    public WebhookDao(SessionFactory factory) {
        super(factory);
    }

    public WebhookEntity findById(Long id) {
        return get(id);
    }

    public long create(WebhookEntity webhook) {
        return persist(webhook).getId();
    }

}

package uk.gov.pay.webhooks.webhook.jpa.dao;

import com.google.inject.Provider;
import jakarta.persistence.EntityManager;
import uk.gov.pay.webhooks.common.jpa.dao.JpaDao;
import uk.gov.pay.webhooks.webhook.jpa.entity.WebhookEntity;

import javax.inject.Inject;

public class WebhookDao extends JpaDao<WebhookEntity> {

    @Inject
    public WebhookDao(final Provider<EntityManager> entityManager) {
        super(entityManager);
    }

}

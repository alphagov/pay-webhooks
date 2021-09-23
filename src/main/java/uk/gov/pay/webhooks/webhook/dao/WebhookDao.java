package uk.gov.pay.webhooks.webhook.dao;

import com.google.inject.Provider;
import javax.persistence.EntityManager;
import uk.gov.pay.webhooks.common.jpa.dao.JpaDao;
import uk.gov.pay.webhooks.webhook.WebhookDTO;
import uk.gov.pay.webhooks.webhook.entity.WebhookEntity;

import javax.inject.Inject;

public class WebhookDao extends JpaDao<WebhookEntity> {
    @Inject
    public WebhookDao(final Provider<EntityManager> entityManager) {
        super(entityManager);
    }

    public WebhookEntity createWebhook(WebhookDTO webhookDTO) {
        WebhookEntity webhook = WebhookEntity.from(webhookDTO);
        persist(webhook);
        return webhook;
    }
}

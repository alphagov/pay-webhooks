package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.inject.Inject;
import java.util.List;

public class WebhookMessageDao extends AbstractDAO<WebhookMessageEntity> {

    @Inject
    public WebhookMessageDao(SessionFactory factory) {
        super(factory);
        }
        
    public WebhookMessageEntity create(WebhookMessageEntity webhookMessage) {
        persist(webhookMessage);
        return webhookMessage;
    }

    public List<WebhookMessageEntity> list(String webhookId) {
        return namedTypedQuery(WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID)
                .setParameter("webhookId", webhookId)
                .getResultList();
    }

    public List<WebhookMessageEntity> list(String webhookId, String deliveryStatus) {
        return namedTypedQuery(WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID_AND_STATUS)
                .setParameter("webhookId", webhookId)
                .setParameter("deliveryStatus", deliveryStatus)
                .getResultList();
    }
}

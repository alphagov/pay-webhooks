package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class WebhookMessageDao extends AbstractDAO<WebhookMessageEntity> {

    public static final int WEBHOOK_MESSAGES_PAGE_SIZE = 10;

    @Inject
    public WebhookMessageDao(SessionFactory factory) {
        super(factory);
        }

    public WebhookMessageEntity create(WebhookMessageEntity webhookMessage) {
        persist(webhookMessage);
        return webhookMessage;
    }

    public Optional<WebhookMessageEntity> get(WebhookEntity webhook, String messageId) {
       return namedTypedQuery(WebhookMessageEntity.MESSAGE_BY_WEBHOOK_ID_AND_MESSAGE_ID)
               .setParameter("webhook", webhook)
               .setParameter("messageId", messageId)
               .stream()
               .findFirst();
    }

    public List<WebhookMessageEntity> list(String webhookId, DeliveryStatus deliveryStatus, int page) {
        var query = deliveryStatus != null ?
                namedTypedQuery(WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID_AND_STATUS).setParameter("webhookId", webhookId).setParameter("deliveryStatus", deliveryStatus) :
                namedTypedQuery(WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID).setParameter("webhookId", webhookId);
        return query.setFirstResult(calculateFirstResult(page))
                .setMaxResults(WEBHOOK_MESSAGES_PAGE_SIZE)
                .getResultList();
    }

    public Long count(String webhookId, DeliveryStatus deliveryStatus) {
        var query = deliveryStatus != null ?
                namedQuery(WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID_AND_STATUS).setParameter("webhookId", webhookId).setParameter("deliveryStatus", deliveryStatus) :
                namedQuery(WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID).setParameter("webhookId", webhookId);
        return (Long) query.getSingleResult();
    }

    private int calculateFirstResult(int page) {
        return (page - 1) * WEBHOOK_MESSAGES_PAGE_SIZE;
    }
}

package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;

import javax.inject.Inject;
import java.util.List;

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

    public List<WebhookMessageEntity> list(String webhookId, String deliveryStatus, int page) {
        var query = (deliveryStatus != null) ?
                namedTypedQuery(WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID_AND_STATUS).setParameter("webhookId", webhookId).setParameter("deliveryStatuses", List.of(deliveryStatus)) :
                namedTypedQuery(WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID).setParameter("webhookId", webhookId);
        return query.setFirstResult(calculateFirstResult(page))
                .setMaxResults(WEBHOOK_MESSAGES_PAGE_SIZE)
                .getResultList();
    }

    public Long count(String webhookId, String deliveryStatus) {
        var query = (deliveryStatus != null) ?
                namedQuery(WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID_AND_STATUS).setParameter("webhookId", webhookId).setParameter("deliveryStatuses", List.of(deliveryStatus)) :
                namedQuery(WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID).setParameter("webhookId", webhookId);
        return (Long) query.getSingleResult();
    }

    private int calculateFirstResult(int page) {
        return (page - 1) * WEBHOOK_MESSAGES_PAGE_SIZE;
    }
}

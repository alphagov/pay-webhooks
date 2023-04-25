package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.inject.Inject;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public List<WebhookMessageEntity> list(WebhookEntity webhook, DeliveryStatus deliveryStatus, int page) {
        var query = deliveryStatus != null ?
                namedTypedQuery(WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID_AND_STATUS)
                        .setParameter("webhook", webhook)
                        .setParameter("deliveryStatus", deliveryStatus) :
                namedTypedQuery(WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID)
                        .setParameter("webhook", webhook);
        return query.setFirstResult(calculateFirstResult(page))
                .setMaxResults(WEBHOOK_MESSAGES_PAGE_SIZE)
                .getResultList();
    }

    public Long count(WebhookEntity webhook, DeliveryStatus deliveryStatus) {
        var query = deliveryStatus != null ?
                namedQuery(WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID_AND_STATUS)
                        .setParameter("webhook", webhook)
                        .setParameter("deliveryStatus", deliveryStatus) :
                namedQuery(WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID)
                        .setParameter("webhook", webhook);
        return (Long) query.getSingleResult();
    }

    private int calculateFirstResult(int page) {
        return (page - 1) * WEBHOOK_MESSAGES_PAGE_SIZE;
    }

    public List<WebhookMessageEntity> getWebhookMessagesOlderThan(int days) {
        return namedTypedQuery(WebhookMessageEntity.MESSAGES_OLDER_THAN_X_DAYS)
                .setParameter("datetime", OffsetDateTime.now().minusDays(days))
                .getResultList();
    }

    public int deleteMessages(Stream<WebhookMessageEntity> webhookMessageEntities) {
        return currentSession().createQuery("delete from WebhookMessageEntity where id in :ids")
                .setParameter("ids", webhookMessageEntities.map(WebhookMessageEntity::getId).collect(Collectors.toList()))
                .executeUpdate();
    }
}

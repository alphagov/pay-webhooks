package uk.gov.pay.webhooks.deliveryqueue.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;

import javax.inject.Inject;
import javax.persistence.LockModeType;
import java.time.InstantSource;
import java.util.Date;
import java.util.Optional;

public class WebhookDeliveryQueueDao extends AbstractDAO<WebhookDeliveryQueueEntity> {
    public final InstantSource instantSource;

    @Inject
    public WebhookDeliveryQueueDao(SessionFactory sessionFactory, InstantSource instantSource) {
        super(sessionFactory);
        this.instantSource = instantSource;
    }

    public Optional<WebhookDeliveryQueueEntity> nextToSend(java.util.Date sendAt) {
        return namedTypedQuery(WebhookDeliveryQueueEntity.NEXT_TO_SEND)
                .setParameter("send_at", sendAt)
                .setMaxResults(1)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint(
                        "javax.persistence.lock.timeout",
                        LockOptions.SKIP_LOCKED
                )
                .getResultList()
                .stream()
                .findAny();
    }

    public WebhookDeliveryQueueEntity enqueueFrom(WebhookMessageEntity webhookMessageEntity, WebhookDeliveryQueueEntity.DeliveryStatus deliveryStatus, Date sendAt) {
       return persist(WebhookDeliveryQueueEntity.enqueueFrom(
                webhookMessageEntity,
                instantSource.instant(),
                deliveryStatus,
                sendAt));
    }
    
    public Long countFailed(WebhookMessageEntity webhookMessageEntity) {
        return (Long) namedQuery(WebhookDeliveryQueueEntity.COUNT_FAILED)
                .setParameter("webhook_message_id", webhookMessageEntity)
                .getSingleResult();
    }

    public WebhookDeliveryQueueEntity recordResult(WebhookDeliveryQueueEntity webhookDeliveryQueueEntity, String deliveryResult, Integer statusCode, WebhookDeliveryQueueEntity.DeliveryStatus deliveryStatus) {
        return persist(WebhookDeliveryQueueEntity.recordResult(webhookDeliveryQueueEntity, deliveryResult, statusCode, deliveryStatus));
    }
}

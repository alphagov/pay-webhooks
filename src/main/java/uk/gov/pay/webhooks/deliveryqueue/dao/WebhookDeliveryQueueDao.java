package uk.gov.pay.webhooks.deliveryqueue.dao;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.deliveryqueue.dao.entity.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;

import javax.inject.Inject;
import javax.persistence.LockModeType;
import java.sql.Date;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Optional;

public class WebhookDeliveryQueueDao extends AbstractDAO<WebhookDeliveryQueueEntity> {
    private final InstantSource instantSource;
    
    @Inject
    public WebhookDeliveryQueueDao(SessionFactory sessionFactory, InstantSource instantSource) {
        super(sessionFactory);
        this.instantSource = instantSource;
    }
    
    public void enqueueAttempt(WebhookMessageEntity webhookMessageEntity) {
        var deliveryAttempt = new WebhookDeliveryQueueEntity();
        deliveryAttempt.setWebhookMessageEntity(webhookMessageEntity);
        deliveryAttempt.setDeliveryStatus(WebhookDeliveryQueueEntity.DeliveryStatus.PENDING.name());
        deliveryAttempt.setCreatedDate(Date.from(instantSource.instant()));
        deliveryAttempt.setSendAt(Date.from(instantSource.instant()));
        persist(deliveryAttempt);
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
}

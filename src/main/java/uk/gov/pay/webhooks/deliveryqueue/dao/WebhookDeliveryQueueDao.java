package uk.gov.pay.webhooks.deliveryqueue.dao;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.LockOptions;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;

import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WebhookDeliveryQueueDao extends AbstractDAO<WebhookDeliveryQueueEntity> {
    public final InstantSource instantSource;
    @Inject
    public WebhookDeliveryQueueDao(SessionFactory sessionFactory, InstantSource instantSource) {
        super(sessionFactory);
        this.instantSource = instantSource;
    }

    public Optional<WebhookDeliveryQueueEntity> nextToSend() {
        return nextToSend(instantSource.instant());
    }

    public Optional<WebhookDeliveryQueueEntity> nextToSend(Instant sendAt) {
        return namedTypedQuery(WebhookDeliveryQueueEntity.NEXT_TO_SEND)
                .setParameter("send_at", OffsetDateTime.ofInstant(sendAt, ZoneOffset.UTC))
                .setMaxResults(1)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint(
                        "jakarta.persistence.lock.timeout",
                        LockOptions.SKIP_LOCKED
                )
                .getResultList()
                .stream()
                .findAny();
    }

    public WebhookDeliveryQueueEntity enqueueFrom(WebhookMessageEntity webhookMessageEntity, DeliveryStatus deliveryStatus, Instant sendAt) {
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

    public WebhookDeliveryQueueEntity recordResult(WebhookDeliveryQueueEntity webhookDeliveryQueueEntity, String deliveryResult, Duration deliveryResponseTime, Integer statusCode, DeliveryStatus deliveryStatus, MetricRegistry metricRegistry) {
        metricRegistry.counter("delivery-status.%s".formatted(deliveryStatus.name()));
        return persist(WebhookDeliveryQueueEntity.recordResult(webhookDeliveryQueueEntity, deliveryResult, deliveryResponseTime, statusCode, deliveryStatus));
    }

    public List<WebhookDeliveryQueueEntity> list(String webhookId, String messageId) {
        return namedTypedQuery(WebhookDeliveryQueueEntity.LIST_DELIVERY_ATTEMPTS)
                .setParameter("webhookId", webhookId)
                .setParameter("messageId", messageId)
                .getResultList();
    }

    public List<WebhookDeliveryQueueEntity> getWebhookDeliveryQueueEntitiesOlderThan(int days) {
        Preconditions.checkArgument(days > 0, "Can only get webhook delivery queue entities older than 0 days.");
        return namedTypedQuery(WebhookDeliveryQueueEntity.ENTRIES_OLDER_THAN_X_DAYS)
                .setParameter("datetime", OffsetDateTime.now().minusDays(days))
                .getResultList();
    }
}

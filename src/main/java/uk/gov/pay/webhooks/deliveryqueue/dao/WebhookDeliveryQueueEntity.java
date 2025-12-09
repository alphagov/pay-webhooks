package uk.gov.pay.webhooks.deliveryqueue.dao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@NamedQuery(
        name = WebhookDeliveryQueueEntity.ENTRIES_OLDER_THAN_X_DAYS,
        query = "select m from WebhookDeliveryQueueEntity m where createdDate < :datetime"
)

@NamedQuery(
        name = WebhookDeliveryQueueEntity.NEXT_TO_SEND,
        query = "select m from WebhookDeliveryQueueEntity m where :send_at > sendAt and m.deliveryStatus = 'PENDING' order by sendAt asc"
)

@NamedQuery(
        name = WebhookDeliveryQueueEntity.COUNT_FAILED,
        query = "select count(m) from WebhookDeliveryQueueEntity m where webhookMessageEntity = :webhook_message_id and deliveryStatus = 'FAILED'"
)

@NamedQuery(
        name = WebhookDeliveryQueueEntity.LIST_DELIVERY_ATTEMPTS,
        query = "select wdq from WebhookDeliveryQueueEntity wdq where webhookMessageEntity.webhookEntity.externalId = :webhookId and webhookMessageEntity.externalId = :messageId order by createdDate desc"
)

@Entity
@SequenceGenerator(name="webhook_delivery_queue_id_seq", sequenceName = "webhook_delivery_queue_id_seq", allocationSize = 1)
@Table(name = "webhook_delivery_queue")



public class WebhookDeliveryQueueEntity {
    public static final String ENTRIES_OLDER_THAN_X_DAYS = "WebhookDeliveryQueue.entries_older_than_x_days";
    public static final String NEXT_TO_SEND = "WebhookDeliveryQueue.next_to_send";
    public static final String COUNT_FAILED = "WebhookDeliveryQueue.count_failed";
    public static final String LIST_DELIVERY_ATTEMPTS = "WebhookDeliveryQueue.list_delivery_attempts";


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhook_delivery_queue_id_seq")
    private Long id;

    @Column(name = "created_date")
    private OffsetDateTime createdDate;

    public WebhookDeliveryQueueEntity() {
    }

    public Long getId() {
        return id;
    }

    public Instant getSendAt() {
        return sendAt.toInstant();
    }

    public void setSendAt(Instant sendAt) {
        this.sendAt = OffsetDateTime.ofInstant(sendAt, ZoneOffset.UTC);
    }

    @Column(name = "send_at")
    private OffsetDateTime sendAt;

    public Instant getCreatedDate() {
        return createdDate.toInstant();
    }

    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    @Column(name = "delivery_status")
    @Enumerated(EnumType.STRING)
    private DeliveryStatus deliveryStatus;

    @Column(name = "delivery_response_time_in_millis")
    private Long deliveryResponseTime;

    public Optional<Duration> getDeliveryResponseTime() {
        return Optional.ofNullable(this.deliveryResponseTime)
                .map(Duration::ofMillis);
    }

    public void setDeliveryResponseTime(Duration deliveryResponseTime) {
        this.deliveryResponseTime = deliveryResponseTime.toMillis();
    }

    public String getDeliveryResult() {
        return deliveryResult;
    }

    public void setDeliveryResult(String deliveryResult) {
        this.deliveryResult = deliveryResult;
    }

    @Column(name = "delivery_result")
    private String deliveryResult;

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    @Column(name = "status_code")
    private Integer statusCode;

    public WebhookMessageEntity getWebhookMessageEntity() {
        return webhookMessageEntity;
    }

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_message_id", updatable = false)
    private WebhookMessageEntity webhookMessageEntity;



    public void setCreatedDate(Instant createdDate) {
        this.createdDate = OffsetDateTime.ofInstant(createdDate, ZoneOffset.UTC);
    }

    public static WebhookDeliveryQueueEntity enqueueFrom(WebhookMessageEntity webhookMessageEntity, Instant createdInstant, DeliveryStatus deliveryStatus, Instant sendAt) {
        var entity = new WebhookDeliveryQueueEntity();
        entity.setCreatedDate(createdInstant);
        entity.setWebhookMessageEntity(webhookMessageEntity);
        entity.setDeliveryStatus(deliveryStatus);
        entity.setSendAt(sendAt);
        return entity;
    }

    public static WebhookDeliveryQueueEntity recordResult(WebhookDeliveryQueueEntity webhookDeliveryQueueEntity, String deliveryResult, Duration deliveryResponseTime, Integer statusCode, DeliveryStatus deliveryStatus) {
        var entity = webhookDeliveryQueueEntity;
        entity.setDeliveryResult(deliveryResult);
        entity.setDeliveryStatus(deliveryStatus);
        entity.setDeliveryResponseTime(deliveryResponseTime);
        Optional.ofNullable(statusCode).ifPresent(entity::setStatusCode);
        return entity;
    }


    public void setWebhookMessageEntity(WebhookMessageEntity webhookMessageEntity) {
        this.webhookMessageEntity = webhookMessageEntity;
    }
    
}

package uk.gov.pay.webhooks.deliveryqueue.dao;

import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@NamedQuery(
        name = WebhookDeliveryQueueEntity.NEXT_TO_SEND,
        query = "select m from WebhookDeliveryQueueEntity m where :send_at <= send_at"
)

@Entity
@SequenceGenerator(name="webhook_delivery_queue_id_seq", sequenceName = "webhook_delivery_queue_id_seq", allocationSize = 1)
@Table(name = "webhook_delivery_queue")



public class WebhookDeliveryQueueEntity {
    public static final String NEXT_TO_SEND = "WebhookDeliveryQueue.next_to_send";


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_types_id_seq")
    private Long id;

    @Column(name = "created_date")
    private Date createdDate;

    public WebhookDeliveryQueueEntity() {
    }

    public Date getSendAt() {
        return sendAt;
    }

    public void setSendAt(Date sendAt) {
        this.sendAt = sendAt;
    }

    @Column(name = "send_at")
    private Date sendAt;

    public enum DeliveryStatus {
        PENDING,
        SUCCESSFUL,
        FAILED
    }


    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }


    @Column(name = "delivery_status")
    private String deliveryStatus;

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

    @ManyToOne
    @JoinColumn(name = "webhook_message_id", updatable = false)
    private WebhookMessageEntity webhookMessageEntity;



    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public static WebhookDeliveryQueueEntity enqueueFrom(WebhookMessageEntity webhookMessageEntity, Instant createdInstant, DeliveryStatus deliveryStatus, Date sendAt) {
        var entity = new WebhookDeliveryQueueEntity();
        entity.setCreatedDate(Date.from(createdInstant));
        entity.setWebhookMessageEntity(webhookMessageEntity);
        entity.setDeliveryStatus(deliveryStatus.name());
        entity.setSendAt(sendAt);
        return entity;
    }

    public static WebhookDeliveryQueueEntity recordResult(WebhookDeliveryQueueEntity webhookDeliveryQueueEntity, String deliveryResult, Optional<Integer> statusCode, DeliveryStatus deliveryStatus) {
        var entity = webhookDeliveryQueueEntity;
        entity.setDeliveryResult(deliveryResult);
        entity.setDeliveryStatus(deliveryStatus.name());
        statusCode.ifPresent(entity::setStatusCode);
        return entity;
    }


    public void setWebhookMessageEntity(WebhookMessageEntity webhookMessageEntity) {
        this.webhookMessageEntity = webhookMessageEntity;
    }
    
}


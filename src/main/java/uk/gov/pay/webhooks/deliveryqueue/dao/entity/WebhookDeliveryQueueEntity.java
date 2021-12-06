package uk.gov.pay.webhooks.deliveryqueue.dao.entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLEnumType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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

    public WebhookMessageEntity getWebhookMessageEntity() {
        return webhookMessageEntity;
    }

    @ManyToOne
    @JoinColumn(name = "webhook_message_id", updatable = false)
    private WebhookMessageEntity webhookMessageEntity;
    
    
    public static WebhookDeliveryQueueEntity from(WebhookMessageEntity webhookMessageEntity, Instant createdInstant, DeliveryStatus deliveryStatus, Date sendAt) {
     var entity = new WebhookDeliveryQueueEntity();
     entity.setCreatedDate(Date.from(createdInstant));
     entity.setWebhookMessageEntity(webhookMessageEntity);
     entity.setDeliveryStatus(deliveryStatus.name());
     return entity;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    

    public void setWebhookMessageEntity(WebhookMessageEntity webhookMessageEntity) {
        this.webhookMessageEntity = webhookMessageEntity;
    }
    
    
    
    
    
}

package uk.gov.pay.webhooks.message.dao.entity;

import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

@Entity
@SequenceGenerator(name="webhook_delivery_attempts_id_seq", sequenceName = "webhook_delivery_attempts_id_seq", allocationSize = 1)
@Table(name = "webhook_delivery_attempts")



public class WebhookDeliveryAttemptEntity {
    public WebhookDeliveryAttemptEntity() {
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_types_id_seq")
    private Long id;

    @Column(name = "created_date")
    private Date createdDate;

    @ManyToOne
    @JoinColumn(name = "webhook_id", updatable = false)
    private WebhookEntity webhookEntity;
    
    @Column(name = "delivery_status")
    private String deliveryStatus;

    @ManyToOne
    @JoinColumn(name = "webhook_message_id", updatable = false)
    private WebhookMessageEntity webhookMessageEntity;
    
    @Column(name = "successful")
    private boolean successful;

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setWebhookEntity(WebhookEntity webhookEntity) {
        this.webhookEntity = webhookEntity;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public void setWebhookMessageEntity(WebhookMessageEntity webhookMessageEntity) {
        this.webhookMessageEntity = webhookMessageEntity;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }
    
    
    
    
}

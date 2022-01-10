package uk.gov.pay.webhooks.message.dao.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
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
import javax.persistence.NamedQuery;
import javax.persistence.FetchType;
import java.util.Date;

@NamedQuery(
        name = WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID,
        query = "select m from WebhookMessageEntity m where webhookEntity.externalId = :webhookId order by createdDate desc"
)

@NamedQuery(
        name = WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID_AND_STATUS,
        query = "select m from WebhookMessageEntity m where webhookEntity.externalId = :webhookId and webhookDeliveryQueueEntity.deliveryStatus in :deliveryStatuses order by createdDate desc"
)

@NamedQuery(
        name = WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID,
        query = "select count(m) from WebhookMessageEntity m where webhookEntity.externalId = :webhookId"
)

@NamedQuery(
        name = WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID_AND_STATUS,
        query = "select count(m) from WebhookMessageEntity m where webhookEntity.externalId = :webhookId and webhookDeliveryQueueEntity.deliveryStatus in :deliveryStatuses"
)

@Entity
@SequenceGenerator(name="webhook_messages_id_seq", sequenceName = "webhook_messages_id_seq", allocationSize = 1)
@Table(name = "webhook_messages")
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonType.class)
})
public class WebhookMessageEntity {

    public static final String MESSAGES_BY_WEBHOOK_ID = "WebhookMessage.messages_by_webhook_id";
    public static final String MESSAGES_BY_WEBHOOK_ID_AND_STATUS = "WebhookMessage.messages_by_webhook_id_and_status";
    public static final String COUNT_MESSAGES_BY_WEBHOOK_ID = "WebhookMessage.count_messages_by_webhook_id";
    public static final String COUNT_MESSAGES_BY_WEBHOOK_ID_AND_STATUS = "WebhookMessage.count_messages_by_webhook_id_and_status";

    public WebhookMessageEntity() {}

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhook_messages_id_seq")
    private Long id;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "created_date")
    private Date createdDate;

    @ManyToOne
    @JoinColumn(name = "webhook_id", updatable = false)
    private WebhookEntity webhookEntity;

    @Column(name = "event_date")
    private Date eventDate;

    @ManyToOne
    @JoinColumn(name = "event_type", updatable = false)
    private EventTypeEntity eventType;
    
    @Type(type = "json")
    @Column(name = "resource", columnDefinition = "json")
    private JsonNode resource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinFormula("""
           (
           SELECT wdq.id
           FROM webhook_delivery_queue wdq
           WHERE wdq.webhook_message_id = id
           AND wdq.delivery_status != 'PENDING'
           ORDER BY wdq.send_at DESC
           LIMIT 1
           )
            """)
    private WebhookDeliveryQueueEntity webhookDeliveryQueueEntity;

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public WebhookEntity getWebhookEntity() {
        return webhookEntity;
    }

    public void setWebhookEntity(WebhookEntity webhookEntity) {
        this.webhookEntity = webhookEntity;
    }

    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }

    public EventTypeEntity getEventType() {
        return eventType;
    }

    public void setEventType(EventTypeEntity eventType) {
        this.eventType = eventType;
    }

    public JsonNode getResource() {
        return resource;
    }

    public void setResource(JsonNode resource) {
        this.resource = resource;
    }

    public WebhookDeliveryQueueEntity getWebhookDeliveryQueueEntity() {
        return webhookDeliveryQueueEntity;
    }
}

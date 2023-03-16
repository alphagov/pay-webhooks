package uk.gov.pay.webhooks.message.dao.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.JoinFormula;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@NamedQuery(
        name = WebhookMessageEntity.MESSAGE_BY_WEBHOOK_ID_AND_MESSAGE_ID,
        query = "select m from WebhookMessageEntity m where webhookEntity = :webhook and externalId = :messageId"
)

@NamedQuery(
        name = WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID,
        query = "select m from WebhookMessageEntity m where webhookEntity = :webhook order by createdDate desc"
)

@NamedQuery(
        name = WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID_AND_STATUS,
        query = "select m from WebhookMessageEntity m where webhookEntity = :webhook and lastDeliveryStatus = :deliveryStatus order by createdDate desc"
)

@NamedQuery(
        name = WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID,
        query = "select count(m) from WebhookMessageEntity m where webhookEntity = :webhook"
)

@NamedQuery(
        name = WebhookMessageEntity.COUNT_MESSAGES_BY_WEBHOOK_ID_AND_STATUS,
        query = "select count(m) from WebhookMessageEntity m where webhookEntity = :webhook and lastDeliveryStatus = :deliveryStatus"
)

@Entity
@SequenceGenerator(name="webhook_messages_id_seq", sequenceName = "webhook_messages_id_seq", allocationSize = 1)
@Table(name = "webhook_messages")
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonType.class)
})
public class WebhookMessageEntity {

    public static final String MESSAGE_BY_WEBHOOK_ID_AND_MESSAGE_ID = "WebhookMessage.message_by_webhook_id_and_message_id";
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

    public String getResourceExternalId() {
        return resourceExternalId;
    }

    public void setResourceExternalId(String resourceExternalId) {
        this.resourceExternalId = resourceExternalId;
    }

    @Column(name = "resource_external_id")
    private String resourceExternalId;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    @Column(name = "resource_type")
    private String resourceType;

    @Column(name = "created_date")
    private OffsetDateTime createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "webhook_id", updatable = false)
    private WebhookEntity webhookEntity;

    @Column(name = "event_date")
    private OffsetDateTime eventDate;

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

    @Column(name = "last_delivery_status")
    @Enumerated(EnumType.STRING)
    private DeliveryStatus lastDeliveryStatus;

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Instant getCreatedDate() {
        return createdDate.toInstant();
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = OffsetDateTime.ofInstant(createdDate, ZoneOffset.UTC);
    }

    public WebhookEntity getWebhookEntity() {
        return webhookEntity;
    }

    public void setWebhookEntity(WebhookEntity webhookEntity) {
        this.webhookEntity = webhookEntity;
    }

    public Instant getEventDate() {
        return eventDate.toInstant();
    }

    public void setEventDate(Instant eventDate) {
        this.eventDate = OffsetDateTime.ofInstant(eventDate, ZoneOffset.UTC);
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

    public DeliveryStatus getLastDeliveryStatus() {
        return lastDeliveryStatus;
    }

    public void setLastDeliveryStatus(DeliveryStatus lastDeliveryStatus) {
        this.lastDeliveryStatus = lastDeliveryStatus;
    }
}

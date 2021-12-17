package uk.gov.pay.webhooks.message.dao.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NamedQuery(
        name = WebhookMessageEntity.SEARCH_BY_STATUS,
        query = "select p from WebhookMessageEntity p where live = :live order by created_date DESC"
)

@NamedQuery(
        name = WebhookMessageEntity.MESSAGES_BY_WEBHOOK_ID,
        query = "select p from WebhookMessageEntity p where external_id = :external_id"
)

@Entity
@SequenceGenerator(name="webhook_messages_id_seq", sequenceName = "webhook_messages_id_seq", allocationSize = 1)
@Table(name = "webhook_messages")
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonType.class)
})
public class WebhookMessageEntity {

    public static final String SEARCH_BY_STATUS = "WebhookMessage.search_by_status";
    public static final String MESSAGES_BY_WEBHOOK_ID = "WebhookMessage.messages_by_webhook_id";
    
    public WebhookMessageEntity() {}

    public List<WebhookDeliveryQueueEntity> getDeliveryAttempts() {
        return deliveryAttempts;
    }
    

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name="webhook_delivery_queue",
            joinColumns=@JoinColumn(name="webhook_message_id", referencedColumnName="id"))
    List<WebhookDeliveryQueueEntity> deliveryAttempts = new ArrayList<>();
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_types_id_seq")
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
}

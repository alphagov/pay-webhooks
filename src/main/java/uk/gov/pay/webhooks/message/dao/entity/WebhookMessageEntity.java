package uk.gov.pay.webhooks.message.dao.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
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
import java.util.Date;

@Entity
@SequenceGenerator(name="webhook_messages_id_seq", sequenceName = "webhook_messages_id_seq", allocationSize = 1)
@Table(name = "webhook_messages")
@TypeDefs({
        @TypeDef(name = "json", typeClass = JsonType.class)
})
public class WebhookMessageEntity {

    public WebhookMessageEntity() {}

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

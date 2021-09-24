package uk.gov.pay.webhooks.webhook.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.webhooks.webhook.CreateWebhookRequest;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;

 
@NamedQuery(
    name = Webhook.GET_BY_EXTERNAL_ID,
    query = "select p from Webhook p where externalId = :externalId"
)
@Entity
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@SequenceGenerator(name="webhooks_id_seq", sequenceName = "webhooks_id_seq", allocationSize = 1)
@Table(name = "webhooks")
public class Webhook {
    public static final String GET_BY_EXTERNAL_ID = "Webhook.get_webhook_by_external_id";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhooks_id_seq")
    private Long id;
    
    @JsonProperty
    @Column(name = "created_date")
    private Date createdDate;
    
    @JsonProperty
    @Column(name = "external_id")
    private String externalId;

    @JsonProperty
    @Column(name = "service_id")
    private String serviceId;

    @JsonProperty
    private boolean live;

    @JsonProperty
    @Column(name = "callback_url")
    private String callbackUrl;

    @JsonProperty
    private String description;

    @JsonProperty
    @Enumerated(EnumType.STRING)
    private WebhookStatus status;

    public static Webhook from(CreateWebhookRequest createWebhookRequest) {
        var entity = new Webhook();
        entity.setDescription(createWebhookRequest.getDescription());
        entity.setCallbackUrl(createWebhookRequest.getCallbackUrl());
        entity.setServiceId(createWebhookRequest.getServiceId());
        entity.setLive(createWebhookRequest.isLive());
        entity.setCreatedDate(Date.from(Instant.now()));
        entity.setStatus(WebhookStatus.ACTIVE);
        entity.setExternalId(new BigInteger(130, new SecureRandom()).toString(32));
        return entity;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    @JsonProperty
    public String getExternalId() {
        return externalId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public boolean isLive() {
        return live;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getDescription() {
        return description;
    }

    public WebhookStatus getStatus() {
        return status;
    }

    private void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    private void setStatus(WebhookStatus status) {
        this.status = status;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setLive(boolean live) {
        this.live = live;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCreatedDate(Date instant) {
        this.createdDate = instant;
    }

    public Long getId() {
        return id;
    }
}

package uk.gov.pay.webhooks.webhook.dao.entity;

import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.resource.CreateWebhookRequest;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@NamedQuery(
    name = WebhookEntity.GET_BY_EXTERNAL_ID_AND_SERVICE_ID,
    query = "select p from WebhookEntity p where externalId = :externalId and serviceId = :serviceId"
)

@NamedQuery(
    name = WebhookEntity.LIST_BY_LIVE_AND_SERVICE_ID,
    query = "select p from WebhookEntity p where live = :live and serviceId = :serviceId"
)

@NamedQuery(
    name = WebhookEntity.LIST_BY_LIVE,
    query = "select p from WebhookEntity p where live = :live"
)
@Entity
@SequenceGenerator(name="webhooks_id_seq", sequenceName = "webhooks_id_seq", allocationSize = 1)
@Table(name = "webhooks")
public class WebhookEntity {
    public static final String GET_BY_EXTERNAL_ID_AND_SERVICE_ID = "Webhook.get_webhook_by_external_id_and_service_id";
    public static final String LIST_BY_LIVE_AND_SERVICE_ID = "Webhook.list_webhooks_by_live_and_service_id";
    public static final String LIST_BY_LIVE = "Webhook.list_webhooks_by_live";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhooks_id_seq")
    private Long id;
    
    @Column(name = "created_date")
    private Date createdDate;
    
    @Column(name = "external_id")
    private String externalId;
    
    @Column(name = "service_id")
    private String serviceId;
    
    private boolean live;
    
    @Column(name = "callback_url")
    private String callbackUrl;
    
    private String description;
    
    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name="webhook_subscriptions",
            joinColumns=@JoinColumn(name="webhook_id", referencedColumnName="id"),
            inverseJoinColumns=@JoinColumn(name="event_type_id", referencedColumnName="id"))
    Set<EventTypeEntity> subscriptions = new HashSet<>();

    
    @Enumerated(EnumType.STRING)
    private WebhookStatus status;

    public static WebhookEntity from(CreateWebhookRequest createWebhookRequest) {
        var entity = new WebhookEntity();
        entity.setDescription(createWebhookRequest.description());
        entity.setCallbackUrl(createWebhookRequest.callbackUrl());
        entity.setServiceId(createWebhookRequest.serviceId());
        entity.setLive(createWebhookRequest.live());
        entity.setCreatedDate(Date.from(Instant.now()));
        entity.setStatus(WebhookStatus.ACTIVE);
        entity.setExternalId(new BigInteger(130, new SecureRandom()).toString(32));
        return entity;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    
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

    public Set<EventTypeEntity> getSubscriptions() {
        return subscriptions;
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

    public void addSubscription(EventTypeEntity eventTypeEntity) {
        this.subscriptions.add(eventTypeEntity);
    }

    public void addSubscriptions(List<EventTypeEntity> subscriptions) {
        subscriptions.forEach(this::addSubscription);
    }
}

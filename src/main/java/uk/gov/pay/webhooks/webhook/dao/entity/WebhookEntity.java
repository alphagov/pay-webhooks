package uk.gov.pay.webhooks.webhook.dao.entity;

import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.resource.CreateWebhookRequest;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@NamedQuery(
    name = WebhookEntity.GET_BY_EXTERNAL_ID_AND_SERVICE_ID,
    query = "select p from WebhookEntity p where externalId = :externalId and serviceId = :serviceId"
)

@NamedQuery(
        name = WebhookEntity.GET_BY_EXTERNAL_ID_AND_GATEWAY_ACCOUNT_ID,
        query = "select p from WebhookEntity p where externalId = :externalId and gatewayAccountId = :gatewayAccountId"
)

@NamedQuery(
        name = WebhookEntity.GET_BY_EXTERNAL_ID,
        query = "select p from WebhookEntity p where externalId = :externalId"
)

@NamedQuery(
    name = WebhookEntity.LIST_BY_LIVE_AND_SERVICE_ID,
    query = "select p from WebhookEntity p where live = :live and serviceId = :serviceId order by createdDate DESC"
)

@NamedQuery(
        name = WebhookEntity.LIST_BY_GATEWAY_ACCOUNT_ID,
        query = "select p from WebhookEntity p where gatewayAccountId = :gatewayAccountId order by createdDate DESC"
)

@NamedQuery(
    name = WebhookEntity.LIST_BY_LIVE,
    query = "select p from WebhookEntity p where live = :live order by createdDate DESC"
)
@Entity
@SequenceGenerator(name="webhooks_id_seq", sequenceName = "webhooks_id_seq", allocationSize = 1)
@Table(name = "webhooks")
public class WebhookEntity {
    public static final String GET_BY_EXTERNAL_ID_AND_SERVICE_ID = "Webhook.get_webhook_by_external_id_and_service_id";
    public static final String GET_BY_EXTERNAL_ID_AND_GATEWAY_ACCOUNT_ID = "Webhook.get_webhook_by_external_id_and_gateway_account_id";
    public static final String GET_BY_EXTERNAL_ID = "Webhook.get_webhook_by_external_id";
    public static final String LIST_BY_LIVE_AND_SERVICE_ID = "Webhook.list_webhooks_by_live_and_service_id";
    public static final String LIST_BY_GATEWAY_ACCOUNT_ID = "Webhook.list_webhooks_by_gateway_account_id";
    public static final String LIST_BY_LIVE = "Webhook.list_webhooks_by_live";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "webhooks_id_seq")
    private Long id;

    @Column(name = "created_date")
    private OffsetDateTime createdDate;

    @Column(name = "external_id")
    private String externalId;

    @Column(name = "service_id")
    private String serviceId;

    @Column(name = "gateway_account_id")
    private String gatewayAccountId;

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

    @Column(name = "signing_key")
    private String signingKey;

    public static WebhookEntity from(CreateWebhookRequest createWebhookRequest, String externalId, Instant createdDate, String webhookSigningKey) {
        var entity = new WebhookEntity();
        entity.setDescription(createWebhookRequest.description());
        entity.setCallbackUrl(createWebhookRequest.callbackUrl());
        entity.setServiceId(createWebhookRequest.serviceId());
        entity.setGatewayAccountId(createWebhookRequest.gatewayAccountId());
        entity.setLive(createWebhookRequest.live());
        entity.setCreatedDate(createdDate);
        entity.setStatus(WebhookStatus.ACTIVE);
        entity.setExternalId(externalId);
        entity.setSigningKey(webhookSigningKey);
        return entity;
    }

    public Instant getCreatedDate() {
        return createdDate.toInstant();
    }

    public String getExternalId() {
        return externalId;
    }

    public String getServiceId() {
        return serviceId;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
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

    public String getSigningKey() {
        return signingKey;
    }

    public Set<EventTypeEntity> getSubscriptions() {
        return subscriptions;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public void setStatus(WebhookStatus status) {
        this.status = status;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public void setGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
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

    public void setCreatedDate(Instant instant) {
        this.createdDate = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
    
    public void setSigningKey(String signingKey) {
        this.signingKey = signingKey;
    }

    public void addSubscription(EventTypeEntity eventTypeEntity) {
        this.subscriptions.add(eventTypeEntity);
    }

    public void addSubscriptions(List<EventTypeEntity> subscriptions) {
        subscriptions.forEach(this::addSubscription);
    }
    
    public void replaceSubscriptions(List<EventTypeEntity> subscriptions) {
        this.subscriptions = new HashSet<>(subscriptions);
    }
}

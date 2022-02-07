package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;
import uk.gov.service.payments.commons.api.json.ApiResponseInstantSerializer;

import java.time.Instant;
import java.util.List;

public record WebhookResponse(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("live") Boolean live,
        @JsonProperty(FIELD_CALLBACK_URL) String callbackUrl,
        @JsonProperty(FIELD_DESCRIPTION) String description,
        @JsonProperty("external_id") String externalId,
        @JsonProperty(FIELD_STATUS) WebhookStatus status,
        @JsonProperty("created_date") @JsonSerialize(using = ApiResponseInstantSerializer.class) Instant createdDate,
        @JsonProperty(FIELD_SUBSCRIPTIONS) List<EventTypeName> subscriptions
) {
    
    public static final String FIELD_CALLBACK_URL = "callback_url";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_SUBSCRIPTIONS = "subscriptions";
    
    public static WebhookResponse from(WebhookEntity webhookEntity) {
        return new WebhookResponse(
                webhookEntity.getServiceId(),
                webhookEntity.isLive(),
                webhookEntity.getCallbackUrl(),
                webhookEntity.getDescription(),
                webhookEntity.getExternalId(),
                webhookEntity.getStatus(),
                webhookEntity.getCreatedDate(),
                webhookEntity.getSubscriptions().stream()
                        .map(EventTypeEntity::getName)
                        .toList()
        );
    }
}

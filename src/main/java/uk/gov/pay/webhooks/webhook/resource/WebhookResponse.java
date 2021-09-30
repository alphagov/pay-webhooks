package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;

import java.util.List;

public record WebhookResponse(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("live") Boolean live,
        @JsonProperty("callback_url") String callbackUrl,
        @JsonProperty("description") String description,
        @JsonProperty("external_id") String externalId,
        @JsonProperty("status") WebhookStatus status,
        @JsonProperty("created_date") String createdDate,
        @JsonProperty("subscriptions") List<EventTypeName> subscriptions
) {
    public static WebhookResponse from(WebhookEntity webhookEntity) {
        return new WebhookResponse(
                webhookEntity.getServiceId(),
                webhookEntity.isLive(),
                webhookEntity.getCallbackUrl(),
                webhookEntity.getDescription(),
                webhookEntity.getExternalId(),
                webhookEntity.getStatus(),
                webhookEntity.getCreatedDate().toString(),
                webhookEntity.getSubscriptions().stream()
                        .map(EventTypeEntity::getName)
                        .toList()
        );
    }
}

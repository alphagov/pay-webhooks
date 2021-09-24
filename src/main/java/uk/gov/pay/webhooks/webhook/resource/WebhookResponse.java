package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;

public record WebhookResponse(
        @JsonProperty("service_id") String serviceId,
        @JsonProperty("live") Boolean live,
        @JsonProperty("callback_url") String callbackUrl,
        @JsonProperty("description") String description,
        @JsonProperty("external_id") String externalId,
        @JsonProperty("status") WebhookStatus status,
        @JsonProperty("created_date") String createdDate
) {
    public static WebhookResponse from(WebhookEntity webhookEntity) {
        return new WebhookResponse(
                webhookEntity.getServiceId(),
                webhookEntity.isLive(),
                webhookEntity.getCallbackUrl(),
                webhookEntity.getDescription(),
                webhookEntity.getExternalId(),
                webhookEntity.getStatus(),
                webhookEntity.getCreatedDate().toString()
        );
    }
}

package uk.gov.pay.webhooks.message.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.service.payments.commons.api.json.ApiResponseInstantSerializer;

import java.time.Instant;

public record WebhookMessageResponse(
        @JsonProperty("external_id") String externalId,
        @JsonProperty("created_date") @JsonSerialize(using = ApiResponseInstantSerializer.class) Instant createdDate,
        @JsonProperty("event_date") @JsonSerialize(using = ApiResponseInstantSerializer.class) Instant eventDate,
        @JsonProperty("event_type") EventTypeName eventTypeName,
        @JsonProperty("resource") JsonNode resource,
        @JsonProperty("latest_attempt") WebhookDeliveryQueueResponse webhookDeliveryQueueEntity) {

    public static WebhookMessageResponse from(WebhookMessageEntity webhookMessageEntity) {
        var latestAttempt = (webhookMessageEntity.getWebhookDeliveryQueueEntity() != null) ? WebhookDeliveryQueueResponse.from(webhookMessageEntity.getWebhookDeliveryQueueEntity()) : null;
        return new WebhookMessageResponse(
                webhookMessageEntity.getExternalId(),
                webhookMessageEntity.getCreatedDate(),
                webhookMessageEntity.getEventDate(),
                webhookMessageEntity.getEventType().getName(),
                webhookMessageEntity.getResource(),
                latestAttempt
        );
    }
}

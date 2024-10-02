package uk.gov.pay.webhooks.message.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.service.payments.commons.api.json.IsoInstantMillisecondSerializer;

import java.time.Instant;

public record WebhookMessageResponse(
        @Schema(example = "s0wjen129ejalk21nfjkdknf1jejklh")
        @JsonProperty("external_id") String externalId,
        @Schema(example = "\"2022-04-05T21:33:45.611Z\"")
        @JsonProperty("created_date") @JsonSerialize(using = IsoInstantMillisecondSerializer.class) Instant createdDate,
        @Schema(example = "\"2022-04-05T21:31:45.611Z\"")
        @JsonProperty("event_date") @JsonSerialize(using = IsoInstantMillisecondSerializer.class) Instant eventDate,
        @Schema(example = "card_payment_started")
        @JsonProperty("event_type") EventTypeName eventTypeName,
        @Schema(example = "payment-external-id-123")
        @JsonProperty("resource_id") String resourceId,
        @Schema(example = "payment")
        @JsonProperty("resource_type") String resourceType,
        @JsonProperty("resource") JsonNode resource,
        @JsonProperty("latest_attempt") WebhookDeliveryQueueResponse webhookDeliveryQueueEntity,
        @JsonProperty("last_delivery_status") DeliveryStatus lastDeliveryStatus) {

    public static WebhookMessageResponse from(WebhookMessageEntity webhookMessageEntity) {
        var latestAttempt = (webhookMessageEntity.getWebhookDeliveryQueueEntity() != null) ? WebhookDeliveryQueueResponse.from(webhookMessageEntity.getWebhookDeliveryQueueEntity()) : null;
        return new WebhookMessageResponse(
                webhookMessageEntity.getExternalId(),
                webhookMessageEntity.getCreatedDate(),
                webhookMessageEntity.getEventDate(),
                webhookMessageEntity.getEventType().getName(),
                webhookMessageEntity.getResourceExternalId(),
                webhookMessageEntity.getResourceType(),
                webhookMessageEntity.getResource(),
                latestAttempt,
                webhookMessageEntity.getLastDeliveryStatus()
        );
    }
}

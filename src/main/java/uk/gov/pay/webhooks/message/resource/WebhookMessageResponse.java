package uk.gov.pay.webhooks.message.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.service.payments.commons.api.json.ApiResponseInstantSerializer;

import java.time.Instant;
import java.util.List;

public record WebhookMessageResponse(
    @JsonProperty("created_date") @JsonSerialize(using = ApiResponseInstantSerializer.class) Instant createdDate,
    @JsonProperty("event_date") @JsonSerialize(using = ApiResponseInstantSerializer.class) Instant eventDate,
    @JsonProperty("event_type") EventTypeName eventTypeName,
    @JsonProperty("resource") JsonNode resource,
    @JsonProperty("delivery_attempts") List<WebhookDeliveryQueueEntity> deliveryAttempts) {
    
    public static WebhookMessageResponse from(WebhookMessageEntity webhookMessageEntity) {
        return new WebhookMessageResponse(
                webhookMessageEntity.getCreatedDate().toInstant(),
                webhookMessageEntity.getEventDate().toInstant(),
                webhookMessageEntity.getEventType().getName(),
                webhookMessageEntity.getResource(),
                webhookMessageEntity.getDeliveryAttempts()
        );
    }
}

    

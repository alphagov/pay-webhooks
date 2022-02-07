package uk.gov.pay.webhooks.message.resource;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.service.payments.commons.api.json.ApiResponseInstantSerializer;

import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WebhookDeliveryQueueResponse(@JsonSerialize(using = ApiResponseInstantSerializer.class) Instant createdDate, @JsonSerialize(using = ApiResponseInstantSerializer.class) Instant sendAt, WebhookDeliveryQueueEntity.DeliveryStatus status, Integer statusCode, String result) {
    public static WebhookDeliveryQueueResponse from(WebhookDeliveryQueueEntity webhookDeliveryQueueEntity) {
        return new WebhookDeliveryQueueResponse(
                webhookDeliveryQueueEntity.getCreatedDate(),
                webhookDeliveryQueueEntity.getSendAt(),
                webhookDeliveryQueueEntity.getDeliveryStatus(),
                webhookDeliveryQueueEntity.getStatusCode(),
                webhookDeliveryQueueEntity.getDeliveryResult()
        );
    }
}

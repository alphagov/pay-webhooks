package uk.gov.pay.webhooks.message.resource;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.service.payments.commons.api.json.IsoInstantMillisecondSerializer;

import java.time.Duration;
import java.time.Instant;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WebhookDeliveryQueueResponse(@Schema(example = "\"2022-04-05T21:37:32.366Z\"") @JsonSerialize(using = IsoInstantMillisecondSerializer.class) Instant createdDate,
                                           @Schema(example = "\"2022-04-05T21:37:34.366Z\"") @JsonSerialize(using = IsoInstantMillisecondSerializer.class) Instant sendAt,
                                           @Schema(example = "SUCCESSFUL") DeliveryStatus status,
                                           @Schema(example = "23") Long responseTime,
                                           @Schema(example = "200") Integer statusCode,
                                           @Schema(example = "200 OK") String result) {
    public static WebhookDeliveryQueueResponse from(WebhookDeliveryQueueEntity webhookDeliveryQueueEntity) {
        return new WebhookDeliveryQueueResponse(
                webhookDeliveryQueueEntity.getCreatedDate(),
                webhookDeliveryQueueEntity.getSendAt(),
                webhookDeliveryQueueEntity.getDeliveryStatus(),
                webhookDeliveryQueueEntity.getDeliveryResponseTime().map(Duration::toMillis).orElse(null),
                webhookDeliveryQueueEntity.getStatusCode(),
                webhookDeliveryQueueEntity.getDeliveryResult()
        );
    }
}

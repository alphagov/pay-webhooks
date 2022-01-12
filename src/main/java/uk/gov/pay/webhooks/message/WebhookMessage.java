package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.queue.InternalEvent;
import uk.gov.service.payments.commons.api.json.ApiResponseInstantSerializer;

import java.time.Instant;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WebhookMessage(String id,
                             @JsonSerialize(using = ApiResponseInstantSerializer.class) Instant createdDate,
                             String resourceId,
                             Integer apiVersion,
                             String resourceType,
                             EventTypeName eventTypeName,
                             JsonNode resource) {

    public static final Integer API_VERSION = 1;

    public static WebhookMessage of(WebhookMessageEntity webhookMessage, InternalEvent event, JsonNode resource) {
        return new WebhookMessage(webhookMessage.getExternalId(),
                webhookMessage.getEventDate().toInstant(),
                event.resourceExternalId(),
                API_VERSION,
                event.resourceType(),
                webhookMessage.getEventType().getName(),
                resource
        );
    }
}

package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.service.payments.commons.api.json.ApiResponseInstantSerializer;

import java.time.Instant;


@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record WebhookMessageBody(String webhookMessageId,
                                 @JsonSerialize(using = ApiResponseInstantSerializer.class) Instant createdDate,
                                 String resourceId,
                                 Integer apiVersion,
                                 String resourceType,
                                 EventTypeName eventType,
                                 JsonNode resource) {

    public static final int API_VERSION = 1;

    public static WebhookMessageBody from(WebhookMessageEntity webhookMessage) {
        return new WebhookMessageBody(webhookMessage.getExternalId(),
                webhookMessage.getEventDate(),
                webhookMessage.getResourceExternalId(),
                API_VERSION,
                webhookMessage.getResourceType(),
                webhookMessage.getEventType().getName(),
                webhookMessage.getResource()
        );
    }
}

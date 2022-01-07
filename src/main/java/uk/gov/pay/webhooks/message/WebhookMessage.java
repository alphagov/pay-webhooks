package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.queue.EventMessage;
import uk.gov.pay.webhooks.queue.InternalEvent;
import uk.gov.pay.webhooks.util.MicrosecondPrecisionInstantDeserializer;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.service.payments.commons.api.json.ApiResponseInstantSerializer;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeDeserializer;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Date;


public record WebhookMessage(String id,
                             @JsonProperty("created_date") @JsonSerialize(using = ApiResponseInstantSerializer.class) Instant createdDate,
                             @JsonProperty("resource_id") String resourceId,
                             @JsonProperty("api_version") Integer apiVersion,
                             @JsonProperty("resource_type") String resourceType,
                             @JsonProperty("event_type") String eventType,
                             JsonNode resource) {

    public static final Integer API_VERSION = 1;

    public static WebhookMessage of(WebhookMessageEntity webhookMessage, InternalEvent event, JsonNode resource) {
        return new WebhookMessage(webhookMessage.getExternalId(),
                event.eventDate(),
                event.resourceExternalId(),
                API_VERSION,
                event.resourceType(),
                event.eventType(),
                resource
        );
    }
}

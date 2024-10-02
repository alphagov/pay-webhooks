package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;
import uk.gov.service.payments.commons.api.json.IsoInstantMillisecondSerializer;

import java.time.Instant;
import java.util.List;

public record WebhookResponse(
        @Schema(example = "eo29upsdkjlk3jpwjj2dfn12")
        @JsonProperty("service_id") String serviceId,
        @Schema(example = "100")
        @JsonProperty("gateway_account_id") String gatewayAccountId,
        @JsonProperty("live") Boolean live,
        @Schema(example = "https://example.com")
        @JsonProperty(FIELD_CALLBACK_URL) String callbackUrl,
        @Schema(example = "Webhook description")
        @JsonProperty(FIELD_DESCRIPTION) String description,
        @Schema(example = "gh0d0923jpsjdf0923jojlsfgkw3seg")
        @JsonProperty("external_id") String externalId,
        @Schema(example = "ACTIVE")
        @JsonProperty(FIELD_STATUS) WebhookStatus status,
        @Schema(example = "2022-04-05T17:07:15.281Z")
        @JsonProperty("created_date") @JsonSerialize(using = IsoInstantMillisecondSerializer.class) Instant createdDate,
        @ArraySchema(schema = @Schema(example = "card_payment_started"))
        @JsonProperty(FIELD_SUBSCRIPTIONS) List<EventTypeName> subscriptions
) {
    
    public static final String FIELD_CALLBACK_URL = "callback_url";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_SUBSCRIPTIONS = "subscriptions";
    
    public static WebhookResponse from(WebhookEntity webhookEntity) {
        return new WebhookResponse(
                webhookEntity.getServiceId(),
                webhookEntity.getGatewayAccountId(),
                webhookEntity.isLive(),
                webhookEntity.getCallbackUrl(),
                webhookEntity.getDescription(),
                webhookEntity.getExternalId(),
                webhookEntity.getStatus(),
                webhookEntity.getCreatedDate(),
                webhookEntity.getSubscriptions().stream()
                        .map(EventTypeEntity::getName)
                        .toList()
        );
    }
}

package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.pay.webhooks.eventtype.EventTypeName;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateWebhookRequest(
        @Schema(example = "eo29upsdkjlk3jpwjj2dfn12")
        @JsonProperty("service_id") @NotEmpty @Size(max = 32) String serviceId,
        @Schema(example = "100")
        @JsonProperty("gateway_account_id") @NotEmpty String gatewayAccountId,
        @JsonProperty("live") @NotNull Boolean live,
        @Schema(example = "https://example.com")
        @JsonProperty("callback_url") @NotEmpty @Size(max = 2048) String callbackUrl,
        @Schema(example = "Webhook description")
        @JsonProperty("description") String description,
        @ArraySchema(schema = @Schema(example = "card_payment_started"))
        @JsonProperty("subscriptions") List<EventTypeName> subscriptions
) {}

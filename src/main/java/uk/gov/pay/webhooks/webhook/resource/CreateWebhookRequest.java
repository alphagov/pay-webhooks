package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

public record CreateWebhookRequest(
        @JsonProperty("service_id") @NotEmpty @Size(max = 30) String serviceId,
        @JsonProperty("live") @NotNull Boolean live,
        @JsonProperty("callback_url") @NotEmpty @Size(max = 2048) String callbackUrl,
        @JsonProperty("description") String description
) {}

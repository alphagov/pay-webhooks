package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record SigningKeyResponse(@Schema(example = "webhook_live_d0sjdkwn1edjals029dd91odndi21fn") @JsonProperty("signing_key") String signingKey) {};

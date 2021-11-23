package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SigningKeyResponse(@JsonProperty("signing_key") String signingKey) {};

package uk.gov.pay.webhooks.webhook.exception;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ErrorResponse(
    WebhooksErrorIdentifier errorIdentifier,
    String message
) {}

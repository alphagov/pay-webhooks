package uk.gov.pay.webhooks.message.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record WebhookMessageSearchResponse(@Schema(example = "100") int count,
                                           @Schema(example = "1") int page,
                                           List<WebhookMessageResponse> results) {
}

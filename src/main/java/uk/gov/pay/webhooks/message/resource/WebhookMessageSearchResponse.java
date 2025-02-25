package uk.gov.pay.webhooks.message.resource;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record WebhookMessageSearchResponse(@Schema(example = "10", description = "The number of results in the current page") int count,
                                           @Schema(example = "1", description= "The page of results") int page,
                                           @Schema(example = "100", description = "The total number of results") long total,
                                           List<WebhookMessageResponse> results) {
}

package uk.gov.pay.webhooks.message.resource;

import java.util.List;

public record WebhookMessageSearchResponse(int total, int count, int page, List<WebhookMessageResponse> results) { }

package uk.gov.pay.webhooks.message;

import uk.gov.pay.webhooks.eventtype.EventTypeName;

import java.util.Map;
import java.util.Optional;

public class EventMapper {
    private static final Map<String, EventTypeName> internalToWebhook = Map.of(
            "CAPTURE_CONFIRMED", EventTypeName.CARD_PAYMENT_CAPTURED
    );

    private static final Map<EventTypeName, String> webhookToInternal = Map.of(
            EventTypeName.CARD_PAYMENT_CAPTURED, "CAPTURE_CONFIRMED"
    );

    public static Optional<String> getInternalEventNameFor(EventTypeName webhookEventTypeName) {
        return Optional.ofNullable(webhookToInternal.get(webhookEventTypeName));
    }

    public static EventTypeName getWebhookEventNameFor(String webhookEventName) {
        return internalToWebhook.get(webhookEventName);
    }
}

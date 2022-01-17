package uk.gov.pay.webhooks.message;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import uk.gov.pay.webhooks.eventtype.EventTypeName;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventMapper {
    private static final Map<String, EventTypeName> internalToWebhook = Map.of(
            "PAYMENT_STARTED", EventTypeName.CARD_PAYMENT_STARTED,
            "AUTHORISATION_SUCCEEDED", EventTypeName.CARD_PAYMENT_SUCCEEDED,
            "CAPTURE_CONFIRMED", EventTypeName.CARD_PAYMENT_CAPTURED,
            "REFUND_SUCCEEDED", EventTypeName.CARD_PAYMENT_REFUNDED
    );

    private static final Map<EventTypeName, String> webhookToInternal = internalToWebhook
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

    private static final List<EventTypeName> childEvents = List.of(EventTypeName.CARD_PAYMENT_REFUNDED);

    public static Optional<String> getInternalEventNameFor(EventTypeName webhookEventTypeName) {
        return Optional.ofNullable(webhookToInternal.get(webhookEventTypeName));
    }

    public static EventTypeName getWebhookEventNameFor(String webhookEventName) {
        return internalToWebhook.get(webhookEventName);
    }

    public static boolean isChildEvent(EventTypeName webhookEventTypeName) {
        return childEvents.contains(webhookEventTypeName);
    }
}

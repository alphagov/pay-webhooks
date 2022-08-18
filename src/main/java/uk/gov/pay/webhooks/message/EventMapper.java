package uk.gov.pay.webhooks.message;

import jdk.jfr.Event;
import jdk.jfr.EventType;
import uk.gov.pay.webhooks.eventtype.EventTypeName;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EventMapper {
    private static final Map<String, EventTypeName> internalToWebhook = Map.ofEntries(
            Map.entry("USER_APPROVED_FOR_CAPTURE", EventTypeName.CARD_PAYMENT_SUCCEEDED),
            Map.entry("SERVICE_APPROVED_FOR_CAPTURE", EventTypeName.CARD_PAYMENT_SUCCEEDED),
            Map.entry("QUEUED_FOR_CAPTURE", EventTypeName.CARD_PAYMENT_SUCCEEDED),
            Map.entry("AUTHORISATION_REJECTED", EventTypeName.CARD_PAYMENT_FAILED),
            Map.entry("AUTHORISATION_CANCELLED", EventTypeName.CARD_PAYMENT_FAILED),
            Map.entry("GATEWAY_ERROR_DURING_AUTHORISATION", EventTypeName.CARD_PAYMENT_FAILED),
            Map.entry("GATEWAY_TIMEOUT_DURING_AUTHORISATION", EventTypeName.CARD_PAYMENT_FAILED),
            Map.entry("UNEXPECTED_GATEWAY_ERROR_DURING_AUTHORISATION", EventTypeName.CARD_PAYMENT_FAILED),
            Map.entry("CANCELLED_BY_EXTERNAL_SERVICE", EventTypeName.CARD_PAYMENT_FAILED),
            Map.entry("CANCELLED_BY_USER", EventTypeName.CARD_PAYMENT_FAILED),
            Map.entry("CANCELLED_BY_EXPIRATION", EventTypeName.CARD_PAYMENT_EXPIRED),
            Map.entry("CAPTURE_CONFIRMED", EventTypeName.CARD_PAYMENT_CAPTURED),
            Map.entry("REFUND_SUCCEEDED", EventTypeName.CARD_PAYMENT_REFUNDED)
    );

    private static final Map<EventTypeName, List<String>> webhookToInternal = internalToWebhook
            .keySet()
            .stream()
            .collect(Collectors.groupingBy(internalToWebhook::get));

    private static final List<EventTypeName> childEvents = List.of(EventTypeName.CARD_PAYMENT_REFUNDED);

    public static Optional<List<String>> getInternalEventNameFor(EventTypeName webhookEventTypeName) {
        return Optional.ofNullable(webhookToInternal.get(webhookEventTypeName));
    }

    public static EventTypeName getWebhookEventNameFor(String webhookEventName) {
        return internalToWebhook.get(webhookEventName);
    }

    public static boolean isChildEvent(EventTypeName webhookEventTypeName) {
        return childEvents.contains(webhookEventTypeName);
    }
}

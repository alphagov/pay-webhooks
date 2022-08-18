package uk.gov.pay.webhooks.message;

import uk.gov.pay.webhooks.eventtype.EventTypeName;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class EventMapper {
    private static final Map<String, EventTypeName> INTERNAL_TO_WEBHOOK = Map.ofEntries(
            entry("USER_APPROVED_FOR_CAPTURE", EventTypeName.CARD_PAYMENT_SUCCEEDED),
            entry("SERVICE_APPROVED_FOR_CAPTURE", EventTypeName.CARD_PAYMENT_SUCCEEDED),
            entry("QUEUED_FOR_CAPTURE", EventTypeName.CARD_PAYMENT_SUCCEEDED),
            entry("AUTHORISATION_REJECTED", EventTypeName.CARD_PAYMENT_FAILED),
            entry("AUTHORISATION_CANCELLED", EventTypeName.CARD_PAYMENT_FAILED),
            entry("GATEWAY_ERROR_DURING_AUTHORISATION", EventTypeName.CARD_PAYMENT_FAILED),
            entry("GATEWAY_TIMEOUT_DURING_AUTHORISATION", EventTypeName.CARD_PAYMENT_FAILED),
            entry("UNEXPECTED_GATEWAY_ERROR_DURING_AUTHORISATION", EventTypeName.CARD_PAYMENT_FAILED),
            entry("CANCELLED_BY_EXTERNAL_SERVICE", EventTypeName.CARD_PAYMENT_FAILED),
            entry("CANCELLED_BY_USER", EventTypeName.CARD_PAYMENT_FAILED),
            entry("CANCELLED_BY_EXPIRATION", EventTypeName.CARD_PAYMENT_EXPIRED),
            entry("CAPTURE_CONFIRMED", EventTypeName.CARD_PAYMENT_CAPTURED),
            entry("REFUND_SUCCEEDED", EventTypeName.CARD_PAYMENT_REFUNDED)
    );

    private static final Map<EventTypeName, Set<String>> WEBHOOK_TO_INTERNAL = INTERNAL_TO_WEBHOOK
            .keySet()
            .stream()
            .collect(Collectors.groupingBy(INTERNAL_TO_WEBHOOK::get, Collectors.toUnmodifiableSet()));

    private static final Set<EventTypeName> CHILD_EVENTS = Set.of(EventTypeName.CARD_PAYMENT_REFUNDED);

    public static Set<String> getInternalEventNamesFor(EventTypeName webhookEventTypeName) {
        return Optional.ofNullable(WEBHOOK_TO_INTERNAL.get(webhookEventTypeName)).orElse(Set.of());
    }

    public static EventTypeName getWebhookEventNameFor(String webhookEventName) {
        return INTERNAL_TO_WEBHOOK.get(webhookEventName);
    }

    public static boolean isChildEvent(EventTypeName webhookEventTypeName) {
        return CHILD_EVENTS.contains(webhookEventTypeName);
    }
}

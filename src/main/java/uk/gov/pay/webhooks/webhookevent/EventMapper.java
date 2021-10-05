package uk.gov.pay.webhooks.webhookevent;

import uk.gov.pay.webhooks.eventtype.EventTypeName;

import java.util.Map;
import java.util.Optional;

public class EventMapper {
    private static final Map<String, EventTypeName> internalToWebhook = Map.of(
            "CAPTURE_CONFIRMED", EventTypeName.PAYMENT_CAPTURED
    );    
    
    private static final Map<EventTypeName, String> webhookToInternal = Map.of(
            EventTypeName.PAYMENT_CAPTURED, "CAPTURE_CONFIRMED"
    );
    
    public static Optional<String> getInternalEventNameFor(EventTypeName webhookEventTypeName) {
        return Optional.ofNullable(webhookToInternal.get(webhookEventTypeName));
    }
}

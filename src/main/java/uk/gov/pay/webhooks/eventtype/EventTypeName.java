package uk.gov.pay.webhooks.eventtype;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.stream.Stream;

public enum EventTypeName {
    @JsonProperty("card_payment_started")
    CARD_PAYMENT_STARTED("card_payment_started"),
    @JsonProperty("card_payment_succeeded")
    CARD_PAYMENT_SUCCEEDED("card_payment_succeeded"),
    @JsonProperty("card_payment_captured")
    CARD_PAYMENT_CAPTURED("card_payment_captured"),
    @JsonProperty("card_payment_refunded")
    CARD_PAYMENT_REFUNDED("card_payment_refunded"),
    @JsonProperty("card_payment_failed")
    CARD_PAYMENT_FAILED("card_payment_failed"),
    @JsonProperty("card_payment_expired")
    CARD_PAYMENT_EXPIRED("card_payment_expired");

    private final String name;
    
    EventTypeName(String name) {
        this.name = name;
    }

    public static EventTypeName of(String name) {
        return Stream.of(EventTypeName.values())
                .filter(n -> n.getName().equals(name))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public String getName() {
        return this.name;
    }
}

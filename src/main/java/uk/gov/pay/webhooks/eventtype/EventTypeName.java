package uk.gov.pay.webhooks.eventtype;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.stream.Stream;

public enum EventTypeName {
    @JsonProperty("card_payment_captured")
    PAYMENT_CAPTURED("card_payment_captured");
    
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

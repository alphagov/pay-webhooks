package uk.gov.pay.webhooks.eventtype;

import java.util.stream.Stream;

public enum EventTypeName {
    CARD_PAYMENT_CAPTURED("card_payment_captured");
    
    private final String name;
    
    EventTypeName(String name) {
        this.name = name;
    }

    public static EventTypeName of(String name) {
        return Stream.of(EventTypeName.values())
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }

    public String getName() {
        return this.name;
    }
}

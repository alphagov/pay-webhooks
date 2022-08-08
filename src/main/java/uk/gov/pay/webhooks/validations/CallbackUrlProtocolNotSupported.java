package uk.gov.pay.webhooks.validations;

public class CallbackUrlProtocolNotSupported extends RuntimeException {

    public CallbackUrlProtocolNotSupported(String message) {
        super(message);
    }

}

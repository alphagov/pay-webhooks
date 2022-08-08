package uk.gov.pay.webhooks.validations;

public class CallbackUrlMalformedException extends RuntimeException {

    public CallbackUrlMalformedException(String message) {
        super(message);
    }

}

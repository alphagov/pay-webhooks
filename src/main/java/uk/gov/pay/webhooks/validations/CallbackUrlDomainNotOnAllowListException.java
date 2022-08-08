package uk.gov.pay.webhooks.validations;

public class CallbackUrlDomainNotOnAllowListException extends RuntimeException {

    public CallbackUrlDomainNotOnAllowListException(String message) {
        super(message);
    }

}

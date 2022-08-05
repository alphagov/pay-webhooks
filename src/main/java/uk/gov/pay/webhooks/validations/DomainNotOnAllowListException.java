package uk.gov.pay.webhooks.validations;

public class DomainNotOnAllowListException extends RuntimeException {

    public DomainNotOnAllowListException(String message) {
        super(message);
    }

}

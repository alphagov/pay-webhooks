package uk.gov.pay.webhooks.validations;

import uk.gov.pay.webhooks.webhook.exception.WebhooksException;

public class CallbackUrlMalformedException extends WebhooksException {

    public CallbackUrlMalformedException(String message) {
        super(message, IDENTIFIER_CALLBACK_URL_MALFORMED);
    }

}

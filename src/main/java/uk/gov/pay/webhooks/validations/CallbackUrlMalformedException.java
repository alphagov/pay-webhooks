package uk.gov.pay.webhooks.validations;

import uk.gov.pay.webhooks.webhook.exception.WebhooksErrorIdentifier;
import uk.gov.pay.webhooks.webhook.exception.WebhooksException;

public class CallbackUrlMalformedException extends WebhooksException {

    public CallbackUrlMalformedException(String message) {
        super(message, WebhooksErrorIdentifier.CALLBACK_URL_MALFORMED);
    }

}

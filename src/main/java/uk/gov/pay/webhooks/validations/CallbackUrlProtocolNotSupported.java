package uk.gov.pay.webhooks.validations;

import uk.gov.pay.webhooks.webhook.exception.WebhooksException;

public class CallbackUrlProtocolNotSupported extends WebhooksException {

    public CallbackUrlProtocolNotSupported(String message) {
        super(message, IDENTIFIER_CALLBACK_URL_PROTOCOL_NOT_SUPPORTED);
    }

}

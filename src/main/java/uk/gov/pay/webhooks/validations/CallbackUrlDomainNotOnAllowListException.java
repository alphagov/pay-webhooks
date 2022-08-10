package uk.gov.pay.webhooks.validations;

import uk.gov.pay.webhooks.webhook.exception.WebhooksErrorIdentifier;
import uk.gov.pay.webhooks.webhook.exception.WebhooksException;

public class CallbackUrlDomainNotOnAllowListException extends WebhooksException {

    public CallbackUrlDomainNotOnAllowListException(String message) {
        super(message, WebhooksErrorIdentifier.CALLBACK_URL_NOT_ON_ALLOW_LIST);
    }

}

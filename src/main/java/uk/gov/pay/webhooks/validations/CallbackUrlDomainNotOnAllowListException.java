package uk.gov.pay.webhooks.validations;

import uk.gov.pay.webhooks.webhook.exception.WebhooksErrorIdentifier;
import uk.gov.pay.webhooks.webhook.exception.WebhooksException;

import java.net.URL;

public class CallbackUrlDomainNotOnAllowListException extends WebhooksException {
    private final URL url;
    public CallbackUrlDomainNotOnAllowListException(String message, URL url) {
        super(message, WebhooksErrorIdentifier.CALLBACK_URL_NOT_ON_ALLOW_LIST);
        this.url = url;
    }

    public URL getUrl() {
        return url;
    }
}

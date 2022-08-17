package uk.gov.pay.webhooks.deliveryqueue;

import uk.gov.pay.webhooks.webhook.exception.WebhooksException;

public class WebhookNotActiveException extends WebhooksException {
    public WebhookNotActiveException(String message) {
        super(message);
    }
}

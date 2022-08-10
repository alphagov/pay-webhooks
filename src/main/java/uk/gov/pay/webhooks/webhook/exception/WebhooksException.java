package uk.gov.pay.webhooks.webhook.exception;

public class WebhooksException extends RuntimeException {
    private WebhooksErrorIdentifier errorIdentifier = WebhooksErrorIdentifier.GENERIC;

    public WebhooksException(String message) {
        super(message);
    }

    public WebhooksException(String message, WebhooksErrorIdentifier errorIdentifier) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public WebhooksErrorIdentifier getErrorIdentifier() {
        return errorIdentifier;
    }
}

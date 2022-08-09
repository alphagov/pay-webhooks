package uk.gov.pay.webhooks.webhook.exception;

public class WebhooksException extends RuntimeException {
    protected static final String IDENTIFIER_GENERIC = "generic";
    protected static final String IDENTIFIER_CALLBACK_URL_NOT_ON_ALLOW_LIST = "callback_url_not_on_allow_list";
    protected static final String IDENTIFIER_CALLBACK_URL_MALFORMED = "callback_url_malformed";
    protected static final String IDENTIFIER_CALLBACK_URL_PROTOCOL_NOT_SUPPORTED = "callback_url_protocol_not_supported";

    private String errorIdentifier = IDENTIFIER_GENERIC;

    public WebhooksException(String message) {
        super(message);
    }

    public WebhooksException(String message, String errorIdentifier) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}

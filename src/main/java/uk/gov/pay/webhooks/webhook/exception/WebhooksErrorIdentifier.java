package uk.gov.pay.webhooks.webhook.exception;

public enum WebhooksErrorIdentifier {
    GENERIC,
    CALLBACK_URL_NOT_ON_ALLOW_LIST,
    CALLBACK_URL_MALFORMED,
    CALLBACK_URL_PROTOCOL_NOT_SUPPORTED
}

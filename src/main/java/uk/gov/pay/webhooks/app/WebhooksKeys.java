package uk.gov.pay.webhooks.app;

// Non-app specific keys should move to java commons
public final class WebhooksKeys {
    private WebhooksKeys() { }

    public static final String WEBHOOK_EXTERNAL_ID = "webhook_external_id";
    public static final String WEBHOOK_CALLBACK_URL = "webhook_callback_url";
    public static final String WEBHOOK_MESSAGE_EXTERNAL_ID = "webhook_message_external_id";
    public static final String RESOURCE_IS_LIVE = "is_live";
    public static final String JOB_BATCH_ID = "job_id";
    public static final String ERROR = "error";
    public static final String ERROR_MESSAGE = "error_message";
    public static final String WEBHOOK_MESSAGE_RETRY_COUNT = "retry_count";
    public static final String STATE_TRANSITION_TO_STATE = "to_state";
    public static final String WEBHOOK_CALLBACK_URL_DOMAIN = "domain";
    public static final String WEBHOOK_MESSAGE_EVENT_INTERNAL_TYPE = "internal_event_type";
    public static final String WEBHOOK_MESSAGE_EVENT_TYPE = "webhook_event_type";
    public static final String WEBHOOK_MESSAGE_TIME_TO_EMIT_IN_MILLIS = "time_to_send_in_millis";
    public static final String WEBHOOK_MESSAGE_ATTEMPT_RESPONSE_REASON = "reason";
}

package uk.gov.pay.webhooks.app.filters;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.slf4j.MDC;

import static uk.gov.pay.webhooks.app.WebhooksKeys.RESOURCE_IS_LIVE;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_EXTERNAL_ID;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_ACCOUNT_ID;
import static uk.gov.service.payments.logging.LoggingKeys.SERVICE_EXTERNAL_ID;

public class LoggingMDCResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        MDC.remove(WEBHOOK_EXTERNAL_ID);
        MDC.remove(WEBHOOK_MESSAGE_EXTERNAL_ID);
        MDC.remove(SERVICE_EXTERNAL_ID);
        MDC.remove(GATEWAY_ACCOUNT_ID);
        MDC.remove(RESOURCE_IS_LIVE);
    }
}

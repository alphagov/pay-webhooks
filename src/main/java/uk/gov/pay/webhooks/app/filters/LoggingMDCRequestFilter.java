package uk.gov.pay.webhooks.app.filters;

import org.slf4j.MDC;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.util.Optional;

import static uk.gov.pay.webhooks.app.WebhooksKeys.RESOURCE_IS_LIVE;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_EXTERNAL_ID;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_EXTERNAL_ID;
import static uk.gov.service.payments.logging.LoggingKeys.SERVICE_EXTERNAL_ID;

public class LoggingMDCRequestFilter implements ContainerRequestFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        getPathParameterFromRequest("webhookExternalId", containerRequestContext)
                .ifPresent(webhookExternalId -> MDC.put(WEBHOOK_EXTERNAL_ID, webhookExternalId));

        getPathParameterFromRequest("webhookMessageExternalId", containerRequestContext)
                .ifPresent(webhookMessageExternalId -> MDC.put(WEBHOOK_MESSAGE_EXTERNAL_ID, webhookMessageExternalId));

        getQueryParameterFromRequest("service_id", containerRequestContext)
                .ifPresent(serviceExternalId -> MDC.put(SERVICE_EXTERNAL_ID, serviceExternalId));

        getQueryParameterFromRequest("live", containerRequestContext)
                .ifPresent(isLive -> MDC.put(RESOURCE_IS_LIVE, isLive));
    }

    private Optional<String> getPathParameterFromRequest(String parameterName, ContainerRequestContext requestContext) {
        return Optional.ofNullable(requestContext.getUriInfo().getPathParameters().getFirst(parameterName));
    }

    private Optional<String> getQueryParameterFromRequest(String queryParameterName, ContainerRequestContext requestContext) {
        return Optional.ofNullable(requestContext.getUriInfo().getQueryParameters().getFirst(queryParameterName));
    }
}

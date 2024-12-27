package uk.gov.pay.webhooks.webhook.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class WebhookExceptionMapper implements ExceptionMapper<WebhooksException> {

    @Override
    public Response toResponse(WebhooksException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getErrorIdentifier(), e.getMessage()))
                .build();
    }
}

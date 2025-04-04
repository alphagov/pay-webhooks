package uk.gov.pay.webhooks.webhook.exception;

import uk.gov.service.payments.commons.api.exception.ValidationException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

    @Override
    public Response toResponse(ValidationException e) {
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode(), String.join(",", e.getErrors())).build();
    }
}

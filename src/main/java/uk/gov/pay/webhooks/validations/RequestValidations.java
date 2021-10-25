package uk.gov.pay.webhooks.validations;

import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;
import uk.gov.service.payments.commons.api.exception.ValidationException;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class RequestValidations {

    public static void throwIfValueNotValidUrl(JsonPatchRequest request) {
        if (!isValidUrl(request)) {
            throw new ValidationException(Collections.singletonList(format("Value for path [%s] must be a HTTPS URL", request.getPath())));
        }
    }    
    
    public static void throwIfValueNotValidStatusEnum(JsonPatchRequest request) {
        if (!isValidStatusEnum(request)) {
            throw new ValidationException(Collections.singletonList(format("Value for path [%s] must be one of %s", request.getPath(), Arrays.stream(WebhookStatus.values()).map(WebhookStatus::getName).collect(Collectors.joining(" or ")))));
        }
    }

    private static boolean isValidUrl(JsonPatchRequest request) {
        var string = request.valueAsString();
        try {
            new URL(string);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
    
    private static boolean isValidStatusEnum(JsonPatchRequest request) {
        try {
            WebhookStatus.of(request.valueAsString());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}


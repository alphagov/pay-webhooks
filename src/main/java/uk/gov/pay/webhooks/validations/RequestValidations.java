package uk.gov.pay.webhooks.validations;

import uk.gov.service.payments.commons.api.exception.ValidationException;
import uk.gov.service.payments.commons.model.jsonpatch.JsonPatchRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

import static java.lang.String.format;

public class RequestValidations {

    public static void throwIfValueNotValidUrl(JsonPatchRequest request) {
        if (!isValidUrl(request)) {
            throw new ValidationException(Collections.singletonList(format("Value for path [%s] must be a HTTPS URL", request.getPath())));
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
}


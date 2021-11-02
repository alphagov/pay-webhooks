package uk.gov.pay.webhooks.validations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import uk.gov.service.payments.commons.api.exception.ValidationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebhookRequestValidatorTest {
    
    @Test
    public void descriptionShouldBeString() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "description",
                        "op", "replace",
                        "value", true)));
        var thrown = assertThrows(ValidationException.class, () -> new WebhookRequestValidator().validateJsonPatch(request));
        assertThat(thrown.getErrors().get(0), is("Value for path [description] must be a string"));
    }     
    
    @Test
    public void emptyStringDescriptionShouldBeValid() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "description",
                        "op", "replace",
                        "value", "")));
        assertDoesNotThrow(() -> new WebhookRequestValidator().validateJsonPatch(request));
    }    
    
    @Test
    public void callbackUrlNotUrlShouldThrowError() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "callback_url",
                        "op", "replace",
                        "value", "foo bar")));
        var thrown = assertThrows(ValidationException.class, () -> new WebhookRequestValidator().validateJsonPatch(request));
        assertThat(thrown.getErrors().get(0), is("Value for path [callback_url] must be a URL"));
    }     
    
    @Test
    public void invalidStatusEnumShouldThrowError() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "status",
                        "op", "replace",
                        "value", "foo bar")));
        var thrown = assertThrows(ValidationException.class, () -> new WebhookRequestValidator().validateJsonPatch(request));
        assertThat(thrown.getErrors().get(0), is("Value for path [status] must be one of ACTIVE or INACTIVE"));
    }

    @Test
    public void validStatusEnumShouldNotThrowError() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "status",
                        "op", "replace",
                        "value", "INACTIVE")));
        assertDoesNotThrow(() -> new WebhookRequestValidator().validateJsonPatch(request));
    }

    @Test
    public void unknownItemInSubscriptionArrayShouldThrowError() throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.readTree(
                """
                        [
                          {
                            "path": "subscriptions",
                            "op": "replace",
                            "value": ["foo"]
                          }
                          ]
                        """
        );
        var thrown = assertThrows(ValidationException.class, () -> new WebhookRequestValidator().validateJsonPatch(request));
        assertThat(thrown.getErrors().get(0), is("Value for path [subscriptions] must be array of [card_payment_captured]"));
    }

    @Test
    public void nonArrayInSubscriptionShouldThrowError() throws JsonProcessingException {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.readTree(
                """
                        [
                          {
                            "path": "subscriptions",
                            "op": "replace",
                            "value": "string"
                          }
                          ]
                        """
        );
        var thrown = assertThrows(ValidationException.class, () -> new WebhookRequestValidator().validateJsonPatch(request));
        assertThat(thrown.getErrors().get(0), is("Value for path [subscriptions] must be array of [card_payment_captured]"));
    }

    @Test
    public void HttpsCallbackUrlShouldNotThrowError() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "callback_url",
                        "op", "replace",
                        "value", "https://gov.uk")));
        assertDoesNotThrow(() -> new WebhookRequestValidator().validateJsonPatch(request));
    }

}

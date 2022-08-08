package uk.gov.pay.webhooks.validations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.service.payments.commons.api.exception.ValidationException;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WebhookRequestValidatorTest {
    private WebhookRequestValidator webhookRequestValidator;

    @Test
    public void descriptionShouldBeString() {
        webhookRequestValidator = new WebhookRequestValidator();
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "description",
                        "op", "replace",
                        "value", true)));
        var thrown = assertThrows(ValidationException.class, () -> webhookRequestValidator.validateJsonPatch(request));
        assertThat(thrown.getErrors().get(0), is("Value for path [description] must be a string"));
    }     

    @Test
    public void emptyStringDescriptionShouldBeValid() {
        webhookRequestValidator = new WebhookRequestValidator();
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "description",
                        "op", "replace",
                        "value", "")));
        assertDoesNotThrow(() -> webhookRequestValidator.validateJsonPatch(request));
    }    
    
    @Test
    public void callbackUrlNotUrlShouldThrowError() {
        webhookRequestValidator = new WebhookRequestValidator();
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "callback_url",
                        "op", "replace",
                        "value", "foo bar")));
        var thrown = assertThrows(ValidationException.class, () -> webhookRequestValidator.validateJsonPatch(request));
        assertThat(thrown.getErrors().get(0), is("Value for path [callback_url] must be a URL"));
    }     
    
    @Test
    public void invalidStatusEnumShouldThrowError() {
        webhookRequestValidator = new WebhookRequestValidator();
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "status",
                        "op", "replace",
                        "value", "foo bar")));
        var thrown = assertThrows(ValidationException.class, () -> webhookRequestValidator.validateJsonPatch(request));
        assertThat(thrown.getErrors().get(0), is("Value for path [status] must be one of ACTIVE or INACTIVE"));
    }

    @Test
    public void validStatusEnumShouldNotThrowError() {
        webhookRequestValidator = new WebhookRequestValidator();
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "status",
                        "op", "replace",
                        "value", "INACTIVE")));
        assertDoesNotThrow(() -> webhookRequestValidator.validateJsonPatch(request));
    }

    @Test
    public void unknownItemInSubscriptionArrayShouldThrowError() throws JsonProcessingException {
        webhookRequestValidator = new WebhookRequestValidator();
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
        var thrown = assertThrows(ValidationException.class, () -> webhookRequestValidator.validateJsonPatch(request));
        assertThat(thrown.getErrors().get(0), is("Value for path [subscriptions] must be array of [card_payment_started, card_payment_succeeded, card_payment_captured, card_payment_refunded]"));
    }

    @Test
    public void nonArrayInSubscriptionShouldThrowError() throws JsonProcessingException {
        webhookRequestValidator = new WebhookRequestValidator();
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
        var thrown = assertThrows(ValidationException.class, () -> webhookRequestValidator.validateJsonPatch(request));
        assertThat(thrown.getErrors().get(0), is("Value for path [subscriptions] must be array of [card_payment_started, card_payment_succeeded, card_payment_captured, card_payment_refunded]"));
    }

    @Test
    public void httpsCallbackUrlShouldNotThrowError() {
        webhookRequestValidator = new WebhookRequestValidator();
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "callback_url",
                        "op", "replace",
                        "value", "https://gov.uk")));
        assertDoesNotThrow(() -> webhookRequestValidator.validateJsonPatch(request));
    }
}

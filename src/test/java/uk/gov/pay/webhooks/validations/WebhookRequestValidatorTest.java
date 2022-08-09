package uk.gov.pay.webhooks.validations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.service.payments.commons.api.exception.ValidationException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebhookRequestValidatorTest {
    private WebhooksConfig webhooksConfig = mock(WebhooksConfig.class);
    private WebhookRequestValidator webhookRequestValidator = new WebhookRequestValidator(new CallbackUrlService(webhooksConfig));

    @BeforeEach
    public void setUp() {
        when(webhooksConfig.getLiveDataAllowDomains()).thenReturn(Set.of("gov.uk"));
    }

    @Test
    public void descriptionShouldBeString() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "description",
                        "op", "replace",
                        "value", true)));
        var thrown = assertThrows(ValidationException.class, () -> webhookRequestValidator.validate(request, false));
        assertThat(thrown.getErrors().get(0), is("Value for path [description] must be a string"));
    }     

    @Test
    public void emptyStringDescriptionShouldBeValid() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "description",
                        "op", "replace",
                        "value", "")));
        assertDoesNotThrow(() -> webhookRequestValidator.validate(request, false));
    }    
    
    @Test
    public void callbackUrlNotUrlShouldThrowError() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "callback_url",
                        "op", "replace",
                        "value", "foo bar")));
        var thrown = assertThrows(CallbackUrlMalformedException.class, () -> webhookRequestValidator.validate(request, false));
    }     
    
    @Test
    public void invalidStatusEnumShouldThrowError() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "status",
                        "op", "replace",
                        "value", "foo bar")));
        var thrown = assertThrows(ValidationException.class, () -> webhookRequestValidator.validate(request, false));
        assertThat(thrown.getErrors().get(0), is("Value for path [status] must be one of ACTIVE or INACTIVE"));
    }

    @Test
    public void validStatusEnumShouldNotThrowError() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "status",
                        "op", "replace",
                        "value", "INACTIVE")));
        assertDoesNotThrow(() -> webhookRequestValidator.validate(request, false));
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
        var thrown = assertThrows(ValidationException.class, () -> webhookRequestValidator.validate(request, false));
        assertThat(thrown.getErrors().get(0), is("Value for path [subscriptions] must be array of [card_payment_started, card_payment_succeeded, card_payment_captured, card_payment_refunded]"));
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
        var thrown = assertThrows(ValidationException.class, () -> webhookRequestValidator.validate(request, false));
        assertThat(thrown.getErrors().get(0), is("Value for path [subscriptions] must be array of [card_payment_started, card_payment_succeeded, card_payment_captured, card_payment_refunded]"));
    }

    @Test
    public void httpsCallbackUrlShouldNotThrowError() {
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "callback_url",
                        "op", "replace",
                        "value", "https://gov.uk")));
        assertDoesNotThrow(() -> webhookRequestValidator.validate(request, false));
    }
}

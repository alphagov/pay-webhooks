package uk.gov.pay.webhooks.validations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.webhook.resource.CreateWebhookRequest;
import uk.gov.service.payments.commons.api.exception.ValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.webhooks.app.WebhooksKeys.RESOURCE_IS_LIVE;
import static uk.gov.service.payments.logging.LoggingKeys.GATEWAY_ACCOUNT_ID;
import static uk.gov.service.payments.logging.LoggingKeys.MDC_REQUEST_ID_KEY;
import static uk.gov.service.payments.logging.LoggingKeys.SERVICE_EXTERNAL_ID;

class WebhookRequestValidatorTest {
    private WebhooksConfig webhooksConfig = mock(WebhooksConfig.class);
    private WebhookRequestValidator webhookRequestValidator = new WebhookRequestValidator(new CallbackUrlService(webhooksConfig));

    @BeforeEach
    public void setUp() {
        when(webhooksConfig.getLiveDataAllowDomains()).thenReturn(Set.of("gov.uk"));
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
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
        assertThat(thrown.getErrors().get(0), is("Value for path [subscriptions] must be array of [card_payment_started, card_payment_succeeded, card_payment_captured, card_payment_refunded, card_payment_failed, card_payment_expired]"));
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
        assertThat(thrown.getErrors().get(0), is("Value for path [subscriptions] must be array of [card_payment_started, card_payment_succeeded, card_payment_captured, card_payment_refunded, card_payment_failed, card_payment_expired]"));
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

    @Test
    void should_populate_validator_specific_mdc_keys_during_create_webhook_validation_and_clear_them_afterwards() {
        var callbackUrlService = mock(CallbackUrlService.class);
        var validatorWithMockedCallbackUrlService = new WebhookRequestValidator(callbackUrlService);

        MDC.put(MDC_REQUEST_ID_KEY, "request-id");

        var createWebhookRequest = new CreateWebhookRequest(
                "new-service-id",
                "new-gateway-account-id",
                false,
                "https://pay.gov.uk",
                "description",
                List.of()
        );

        doAnswer(_ -> {
            assertEquals("request-id", MDC.get(MDC_REQUEST_ID_KEY));
            assertEquals("new-gateway-account-id", MDC.get(GATEWAY_ACCOUNT_ID));
            assertEquals("new-service-id", MDC.get(SERVICE_EXTERNAL_ID));
            assertEquals("false", MDC.get(RESOURCE_IS_LIVE));
            return null;
        }).when(callbackUrlService).validateCallbackUrl("https://pay.gov.uk", false);

        assertDoesNotThrow(() -> validatorWithMockedCallbackUrlService.validate(createWebhookRequest));

        assertThat(MDC.get(MDC_REQUEST_ID_KEY), is("request-id"));
        assertNull(MDC.get(GATEWAY_ACCOUNT_ID));
        assertNull(MDC.get(SERVICE_EXTERNAL_ID));
        assertNull(MDC.get(RESOURCE_IS_LIVE));
    }
}

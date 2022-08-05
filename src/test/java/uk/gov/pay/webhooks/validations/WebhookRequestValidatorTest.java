package uk.gov.pay.webhooks.validations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.service.payments.commons.api.exception.ValidationException;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookRequestValidatorTest {

    @Mock
    private WebhooksConfig webhooksConfig;
    private WebhookRequestValidator webhookRequestValidator;

    @Test
    public void callbackUrlWithExactDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertDoesNotThrow(() -> webhookRequestValidator.validateUrlIsInLiveDomains("https://gov.uk/my/callback/endpoint"));
    }

    @Test
    public void callbackUrlWithFinalDotAndExactDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertDoesNotThrow(() -> webhookRequestValidator.validateUrlIsInLiveDomains("https://gov.uk./my/callback/endpoint"));
    }

    @Test
    public void callbackUrlWithExactDomainButDifferentCaseInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertDoesNotThrow(() -> webhookRequestValidator.validateUrlIsInLiveDomains("https://GOV.UK/my/callback/endpoint"));
    }

    @Test
    public void callbackUrlWithPortAndExactDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertDoesNotThrow(() -> webhookRequestValidator.validateUrlIsInLiveDomains("https://gov.uk:443/my/callback/endpoint"));
    }
    
    @Test
    public void callbackUrlWithDomainThatIsSingleSubdomainOfDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertDoesNotThrow(() -> webhookRequestValidator.validateUrlIsInLiveDomains("https://www.gov.uk/my/callback/endpoint"));
    }

    @Test
    public void callbackUrlWithDomainThatIsMultipleSubdomainsOfDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertDoesNotThrow(() -> webhookRequestValidator.validateUrlIsInLiveDomains("https://www.example.service.gov.uk/my/callback/endpoint"));
    }

    @Test
    public void callbackUrlWithExactIdnDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("网络.test"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertDoesNotThrow(() -> webhookRequestValidator.validateUrlIsInLiveDomains("https://网络.test/my/callback/endpoint"));
    }

    @Test
    public void callbackUrlWithExactPunycodeDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("xn--io0a7i.test"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertDoesNotThrow(() -> webhookRequestValidator.validateUrlIsInLiveDomains("https://xn--io0a7i.test/my/callback/endpoint"));
    }

    @Test
    public void callbackUrlWithDomainNotInAllowListShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> webhookRequestValidator.validateUrlIsInLiveDomains("https://www.url.service.test/is/not/in/list"));
    }

    @Test
    public void callbackUrlWithDomainWhereBegiiningIsInAllowListShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> webhookRequestValidator.validateUrlIsInLiveDomains("https://gov.uk.test/is/not/in/list"));
    }

    @Test
    public void callbackUrlWithIdnDomainWherePunycodeEquivalentInAllowListShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("xn--io0a7i.test"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> webhookRequestValidator.validateUrlIsInLiveDomains("https://网络.test/is/not/in/list"));
    }

    @Test
    public void callbackUrlWithIPunycodeDomainWhereIdnEquivalentInAllowListShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("网络.test"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> webhookRequestValidator.validateUrlIsInLiveDomains("https://xn--io0a7i.test/is/not/in/list"));
    }

    @Test
    public void callbackUrlWithSinglePartDomainWithExactDomainInAllowListShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("test"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> webhookRequestValidator.validateUrlIsInLiveDomains("https://test/is/not/in/list"));
    }

    @Test
    public void callbackUrlWithIpv4AddressShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> webhookRequestValidator.validateUrlIsInLiveDomains("https://203.0.113.1/is/not/in/list"));
    }

    @Test
    public void callbackUrlWithIpv6AddressShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> webhookRequestValidator.validateUrlIsInLiveDomains("https://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]/is/not/in/list"));
    }

    @Test
    public void callbackUrlWithNoHostShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> webhookRequestValidator.validateUrlIsInLiveDomains("https:///is/not/in/list"));
    }

    @Test
    public void descriptionShouldBeString() {
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
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
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "description",
                        "op", "replace",
                        "value", "")));
        assertDoesNotThrow(() -> webhookRequestValidator.validateJsonPatch(request));
    }    
    
    @Test
    public void callbackUrlNotUrlShouldThrowError() {
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
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
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
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
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "status",
                        "op", "replace",
                        "value", "INACTIVE")));
        assertDoesNotThrow(() -> webhookRequestValidator.validateJsonPatch(request));
    }

    @Test
    public void unknownItemInSubscriptionArrayShouldThrowError() throws JsonProcessingException {
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
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
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
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
        webhookRequestValidator = new WebhookRequestValidator(webhooksConfig);
        var objectMapper = new ObjectMapper();
        JsonNode request = objectMapper.valueToTree(
                Collections.singletonList(Map.of("path", "callback_url",
                        "op", "replace",
                        "value", "https://gov.uk")));
        assertDoesNotThrow(() -> webhookRequestValidator.validateJsonPatch(request));
    }

}

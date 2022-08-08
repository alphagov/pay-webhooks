package uk.gov.pay.webhooks.validations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.webhooks.app.WebhooksConfig;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CallbackUrlServiceTest {

    @Mock
    private WebhooksConfig webhooksConfig;
    private CallbackUrlService callbackUrlService;

    @Test
    public void callbackUrlWithExactDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertDoesNotThrow(() -> callbackUrlService.validateCallbackUrl("https://gov.uk/my/callback/endpoint", true));
    }

    @Test
    public void callbackUrlWithFinalDotAndExactDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertDoesNotThrow(() -> callbackUrlService.validateCallbackUrl("https://gov.uk./my/callback/endpoint", true));
    }

    @Test
    public void callbackUrlWithExactDomainButDifferentCaseInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertDoesNotThrow(() -> callbackUrlService.validateCallbackUrl("https://GOV.UK/my/callback/endpoint", true));
    }

    @Test
    public void callbackUrlWithPortAndExactDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertDoesNotThrow(() -> callbackUrlService.validateCallbackUrl("https://gov.uk:443/my/callback/endpoint", true));
    }

    @Test
    public void callbackUrlWithDomainThatIsSingleSubdomainOfDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertDoesNotThrow(() -> callbackUrlService.validateCallbackUrl("https://www.gov.uk/my/callback/endpoint", true));
    }

    @Test
    public void callbackUrlWithDomainThatIsMultipleSubdomainsOfDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertDoesNotThrow(() -> callbackUrlService.validateCallbackUrl("https://www.example.service.gov.uk/my/callback/endpoint", true));
    }

    @Test
    public void callbackUrlWithExactIdnDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("网络.test"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertDoesNotThrow(() -> callbackUrlService.validateCallbackUrl("https://网络.test/my/callback/endpoint", true));
    }

    @Test
    public void callbackUrlWithExactPunycodeDomainInAllowListShouldBeAllowed() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("xn--io0a7i.test"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertDoesNotThrow(() -> callbackUrlService.validateCallbackUrl("https://xn--io0a7i.test/my/callback/endpoint", true));
    }

    @Test
    public void callbackUrlWithDomainNotInAllowListShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> callbackUrlService.validateCallbackUrl("https://www.url.service.test/is/not/in/list", true));
    }

    @Test
    public void callbackUrlWithDomainWhereBeginningIsInAllowListShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> callbackUrlService.validateCallbackUrl("https://gov.uk.test/is/not/in/list", true));
    }

    @Test
    public void callbackUrlWithIdnDomainWherePunycodeEquivalentInAllowListShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("xn--io0a7i.test"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> callbackUrlService.validateCallbackUrl("https://网络.test/is/not/in/list", true));
    }

    @Test
    public void callbackUrlWithIPunycodeDomainWhereIdnEquivalentInAllowListShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("网络.test"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> callbackUrlService.validateCallbackUrl("https://xn--io0a7i.test/is/not/in/list", true));
    }

    @Test
    public void callbackUrlWithSinglePartDomainWithExactDomainInAllowListShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("test"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> callbackUrlService.validateCallbackUrl("https://test/is/not/in/list", true));
    }

    @Test
    public void callbackUrlWithIpv4AddressShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> callbackUrlService.validateCallbackUrl("https://203.0.113.1/is/not/in/list", true));
    }

    @Test
    public void callbackUrlWithIpv6AddressShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertThrows(DomainNotOnAllowListException.class,
                () -> callbackUrlService.validateCallbackUrl("https://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]/is/not/in/list", true));
    }

    @Test
    public void callbackUrlWithNoHostShouldThrowException() {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertThrows(CallbackUrlMalformedException.class,
                () -> callbackUrlService.validateCallbackUrl("https:///is/not/in/list", true));
    }

    @ParameterizedTest
    @ValueSource(strings = { "http", "ftp", "file" })
    public void callbackUrlWithInvalidProtocolShouldThrowException(String protocol) {
        when(webhooksConfig.getLiveDataAllowHosts()).thenReturn(Set.of("gov.uk"));
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertThrows(CallbackUrlProtocolNotSupported.class,
                () -> callbackUrlService.validateCallbackUrl(protocol + "://gov.uk", true));
    }

    @Test
    public void callbackUrlWithInvalidFormatShouldThrowException() {
        callbackUrlService = new CallbackUrlService(webhooksConfig);
        assertThrows(CallbackUrlMalformedException.class, () -> callbackUrlService.validateCallbackUrl("https:/amalformedurl", true));
    }
}

package uk.gov.pay.webhooks.app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalRestClientFactoryTest {
    @Mock
    InternalRestClientConfig internalRestClientConfig;

    @Test
    void useSSLWhenSecureInternalFlagIsEnabled() {
        when(internalRestClientConfig.isDisabledSecureConnection()).thenReturn(false);
        assertThat(InternalRestClientFactory.buildClient(internalRestClientConfig).getSslContext().getProtocol(), is("TLSv1.2"));
    }

    @Test
    void doesNotUseSSLWhenSecureInternalFlagIsDisabled() {
        when(internalRestClientConfig.isDisabledSecureConnection()).thenReturn(true);
        assertThat(InternalRestClientFactory.buildClient(internalRestClientConfig).getSslContext().getProtocol(), is(not("TLSv1.2")));
    }
}

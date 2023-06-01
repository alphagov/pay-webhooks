package uk.gov.pay.webhooks.app;

import uk.gov.service.payments.logging.RestClientLoggingFilter;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static java.lang.String.format;

public class InternalRestClientFactory {
    private static final String TLSV1_2 = "TLSv1.2";

    public static Client buildClient(InternalRestClientConfig clientConfig) {
        ClientBuilder clientBuilder = ClientBuilder.newBuilder();

        if (!clientConfig.isDisabledSecureConnection()) {
            try {
                SSLContext sslContext = SSLContext.getInstance(TLSV1_2);
                sslContext.init(null, null, null);
                clientBuilder = clientBuilder.sslContext(sslContext);
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(format("Unable to find an SSL context for %s", TLSV1_2), e);
            }
        }

        Client client = clientBuilder.build();
        client.register(RestClientLoggingFilter.class);

        return client;
    }

    private InternalRestClientFactory() {
    }
}

package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static uk.gov.pay.webhooks.message.WebhookMessageSender.SIGNATURE_HEADER_NAME;
import static uk.gov.pay.webhooks.message.WebhookMessageSender.TIMEOUT;

@ExtendWith(MockitoExtension.class)
class WebhookMessageSenderTest {

    private static final String PAYLOAD = """
            {
                "json": "and",
                "the": "argonauts"
            }
            """;

    private static final URI CALLBACK_URL = URI.create("http://www.callbackurl.test/webhook-handler");
    private static final String SIGNING_KEY = "Signing key";
    private static final String SIGNATURE = "Signature";

    @Mock
    private HttpClient mockHttpClient;

    @Mock
    private WebhookMessageSignatureGenerator mockWebhookMessageSignatureGenerator;

    @Mock
    private HttpResponse<String> mockHttpResponse;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebhookMessageSender webhookMessageSender;
    private WebhookMessageEntity webhookMessageEntity;
    private WebhookEntity webhookEntity;
    private JsonNode jsonPayload;

    @BeforeEach
    void setUp() throws JsonProcessingException, InvalidKeyException {
        jsonPayload = objectMapper.readTree(PAYLOAD);

        webhookEntity = new WebhookEntity(); 
        webhookEntity.setCallbackUrl(CALLBACK_URL.toString());
        webhookEntity.setSigningKey(SIGNING_KEY);

        webhookMessageEntity = new WebhookMessageEntity();
        webhookMessageEntity.setWebhookEntity(webhookEntity);
        webhookMessageEntity.setResource(jsonPayload);

        given(mockWebhookMessageSignatureGenerator.generate(jsonPayload.toString(), SIGNING_KEY)).willReturn(SIGNATURE);

        webhookMessageSender = new WebhookMessageSender(mockHttpClient, objectMapper, mockWebhookMessageSignatureGenerator);
    }

    @Test
    void constructsHttpRequestAndReturnsHttpResponse() throws IOException, InvalidKeyException, InterruptedException {
        var httpRequestArgumentCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        given(mockHttpClient.send(httpRequestArgumentCaptor.capture(), eq(HttpResponse.BodyHandlers.ofString()))).willReturn(mockHttpResponse);

        HttpResponse<String> result = webhookMessageSender.sendWebhookMessage(webhookMessageEntity);

        HttpRequest httpRequest = httpRequestArgumentCaptor.getValue();
        assertThat(httpRequest.uri(), is(CALLBACK_URL));
        assertThat(httpRequest.timeout(), is(Optional.of(TIMEOUT)));
        assertThat(httpRequest.headers().firstValue("Content-Type"), is(Optional.of("application/json")));
        assertThat(httpRequest.headers().firstValue(SIGNATURE_HEADER_NAME), is(Optional.of(SIGNATURE)));

        assertThat(result, is(mockHttpResponse));
    }

    @Test
    void propagatesIOException() throws IOException, InterruptedException {
        given(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).willThrow(IOException.class);
        assertThrows(IOException.class, () -> webhookMessageSender.sendWebhookMessage(webhookMessageEntity));
    }

    @Test
    void propagatesInterruptedException() throws IOException, InterruptedException {
        given(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).willThrow(InterruptedException.class);
        assertThrows(InterruptedException.class, () -> webhookMessageSender.sendWebhookMessage(webhookMessageEntity));
    }

    @Test
    void propagatesInvalidKeyException() throws InvalidKeyException {
        given(mockWebhookMessageSignatureGenerator.generate(jsonPayload.toString(), SIGNING_KEY)).willThrow(InvalidKeyException.class);
        assertThrows(InvalidKeyException.class, () -> webhookMessageSender.sendWebhookMessage(webhookMessageEntity));
    }

}

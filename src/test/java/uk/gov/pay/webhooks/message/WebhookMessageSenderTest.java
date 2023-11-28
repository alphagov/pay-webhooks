package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.validations.CallbackUrlDomainNotOnAllowListException;
import uk.gov.pay.webhooks.validations.CallbackUrlService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookStatus;

import java.io.IOException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static uk.gov.pay.webhooks.message.WebhookMessageSender.SIGNATURE_HEADER_NAME;

@ExtendWith(MockitoExtension.class)
class WebhookMessageSenderTest {

    private static final String PAYLOAD = """
        {
            "id": "externalId",
            "created_date": "2022-01-12T17:31:06.809Z",
            "resource_id": "foo",
            "api_version": 1,
            "resource_type": null,
            "event_type_name": "card_payment_captured",
            "resource": {
                "json": "and",
                "the": "argonauts"
            }
        }
            """;

    private static final URI CALLBACK_URL = URI.create("http://www.callbackurl.test/webhook-handler");
    private static final String SIGNING_KEY = "Signing key";
    private static final String SIGNATURE = "Signature";

    @Mock
    private CloseableHttpClient mockHttpClient;

    @Mock
    private WebhookMessageSignatureGenerator mockWebhookMessageSignatureGenerator;

    @Mock
    private CallbackUrlService callbackUrlService;

    @Mock
    private CloseableHttpResponse mockHttpResponse;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private WebhookMessageSender webhookMessageSender;
    private WebhookMessageEntity webhookMessageEntity;
    private WebhookEntity webhookEntity;
    private JsonNode jsonPayload;

    @BeforeEach
    void setUp() throws JsonProcessingException, InvalidKeyException {
        jsonPayload = objectMapper.readTree(PAYLOAD);

        webhookEntity = new WebhookEntity(); 
        webhookEntity.setStatus(WebhookStatus.ACTIVE);
        webhookEntity.setCallbackUrl(CALLBACK_URL.toString());
        webhookEntity.setSigningKey(SIGNING_KEY);
        webhookEntity.setLive(true);

        webhookMessageEntity = new WebhookMessageEntity();
        webhookMessageEntity.setWebhookEntity(webhookEntity);
        webhookMessageEntity.setResource(jsonPayload);
        webhookMessageEntity.setEventDate(Instant.parse("2019-10-01T08:25:24.00Z"));
        EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
        webhookMessageEntity.setEventType(eventTypeEntity);
        webhookMessageEntity.setResourceExternalId("foo");
        webhookMessageEntity.setExternalId("externalId");
        webhookMessageEntity.setResourceType("payment");

        webhookMessageSender = new WebhookMessageSender(mockHttpClient, objectMapper, callbackUrlService, mockWebhookMessageSignatureGenerator);
    }

    @Test
    void constructsHttpRequestAndReturnsHttpResponse() throws IOException, InvalidKeyException, InterruptedException {
        var httpRequestArgumentCaptor = ArgumentCaptor.forClass(HttpUriRequest.class);
        given(mockWebhookMessageSignatureGenerator.generate(objectMapper.writeValueAsString(WebhookMessageBody.from(webhookMessageEntity)), SIGNING_KEY)).willReturn(SIGNATURE);
        given(mockHttpClient.execute(httpRequestArgumentCaptor.capture())).willReturn(mockHttpResponse);

        HttpResponse result = webhookMessageSender.sendWebhookMessage(webhookMessageEntity);

        HttpUriRequest httpRequest = httpRequestArgumentCaptor.getValue();
        assertThat(httpRequest.getURI(), is(CALLBACK_URL));
        assertThat(httpRequest.getFirstHeader("Content-Type").getValue(), is("application/json"));
        assertThat(httpRequest.getFirstHeader(SIGNATURE_HEADER_NAME).getValue(), is(SIGNATURE));
        assertThat(result, is(mockHttpResponse));
    }

    @Test
    void propagatesIOException() throws IOException, InterruptedException, InvalidKeyException {
        given(mockHttpClient.execute(any(HttpUriRequest.class))).willThrow(IOException.class);
        given(mockWebhookMessageSignatureGenerator.generate(objectMapper.writeValueAsString(WebhookMessageBody.from(webhookMessageEntity)), SIGNING_KEY)).willReturn(SIGNATURE);
        assertThrows(IOException.class, () -> webhookMessageSender.sendWebhookMessage(webhookMessageEntity));
    }

    @Test
    void propagatesInvalidKeyException() throws InvalidKeyException, JsonProcessingException {
        given(mockWebhookMessageSignatureGenerator.generate(objectMapper.writeValueAsString(WebhookMessageBody.from(webhookMessageEntity)), SIGNING_KEY)).willThrow(InvalidKeyException.class);
        assertThrows(InvalidKeyException.class, () -> webhookMessageSender.sendWebhookMessage(webhookMessageEntity));
    }

    @Test
    void propagatesAndValidatesForLiveDataCallbackUrlDomainNotOnAllowListException() {
        doThrow(CallbackUrlDomainNotOnAllowListException.class).when(callbackUrlService).validateCallbackUrl(webhookEntity.getCallbackUrl(), webhookEntity.isLive());
        assertThrows(CallbackUrlDomainNotOnAllowListException.class, () -> webhookMessageSender.sendWebhookMessage(webhookMessageEntity));
    }

}

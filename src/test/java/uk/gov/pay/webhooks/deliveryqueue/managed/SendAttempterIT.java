package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.dropwizard.core.setup.Environment;
import io.github.netmikey.logunit.api.LogCapturer;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.message.HttpPostFactory;
import uk.gov.pay.webhooks.message.WebhookMessageSender;
import uk.gov.pay.webhooks.message.WebhookMessageSignatureGenerator;
import uk.gov.pay.webhooks.validations.CallbackUrlService;

import java.time.Instant;
import java.time.InstantSource;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER;
import static com.github.tomakehurst.wiremock.http.Fault.EMPTY_RESPONSE;
import static com.github.tomakehurst.wiremock.http.Fault.RANDOM_DATA_THEN_CLOSE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntityFixture.aWebhookDeliveryQueueEntity;
import static uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntityFixture.aWebhookMessageEntity;
import static uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntityFixture.aWebhookEntity;

@WireMockTest
@ExtendWith(MockitoExtension.class)
class SendAttempterIT {

    @RegisterExtension
    LogCapturer logs = LogCapturer.create().captureForLogger(ROOT_LOGGER_NAME);

    @Mock
    private WebhookDeliveryQueueDao mockWebhookDeliveryQueueDao;

    private static final String CALLBACK_URL = "/callback-url";
    private static final String NOW = "2022-01-01T00:00:00.000Z";
    private SendAttempter sendAttempter;
    private WebhookDeliveryQueueEntity enqueuedItem;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        sendAttempter = buildSendAttempter();
        enqueuedItem = buildWebhookDeliveryQueueEntityWithCallbackURL(wmRuntimeInfo.getHttpBaseUrl() + CALLBACK_URL);
    }

    @Nested
    class when_send_successful {

        @BeforeEach
        void setUp() {
            givenThat(post(CALLBACK_URL)
                    .willReturn(ok()));
        }

        @Test
        void should_POST_item_to_callback_URL() {
            sendAttempter.attemptSend(enqueuedItem);

            verify(postRequestedFor(urlEqualTo(CALLBACK_URL))
                    .withRequestBody(equalToJson("""
                            {
                              "webhook_message_id": null,
                              "created_date": "%s",
                              "resource_id": null,
                              "api_version": 1,
                              "resource_type": null,
                              "event_type": null,
                              "resource": null
                            }
                            """.formatted(NOW))));
        }

        @Test
        void should_log_at_start_and_end_of_send_attempt() {
            sendAttempter.attemptSend(enqueuedItem);

            assertThat(logs.size(), is(2));
            logs.assertContains("Sending webhook message started");
            logs.assertContains("Sending webhook message finished");
        }
    }

    @Test
    void should_log_connection_reset_by_peer_fault_with_error_message() {
        givenThat(post(CALLBACK_URL)
                .willReturn(aResponse()
                        .withFault(CONNECTION_RESET_BY_PEER)));

        sendAttempter.attemptSend(enqueuedItem);

        var loggingEvent = logs.assertContains("Exception caught by request");
        var markers = loggingEvent.getMarkers();
        assertThat(markers.size(), is(1));
        assertThat(markers.getFirst(), hasToString("error_message=Connection reset"));
    }

    @Test
    void should_log_when_response_is_random_data_and_the_connection_is_closed() {
        givenThat(post(CALLBACK_URL)
                .willReturn(aResponse()
                        .withFault(RANDOM_DATA_THEN_CLOSE)));

        sendAttempter.attemptSend(enqueuedItem);

        var loggingEvent = logs.assertContains("Exception caught by request");
        var markers = loggingEvent.getMarkers();
        assertThat(markers.size(), is(1));
        assertThat(markers.getFirst(), hasToString("error_message=null"));
    }

    @Test
    void should_log_empty_response_fault() {
        givenThat(post(CALLBACK_URL)
                .willReturn(aResponse()
                        .withFault(EMPTY_RESPONSE)));

        sendAttempter.attemptSend(enqueuedItem);

        logs.assertContains("Request timed out");
    }

    @ParameterizedTest
    @EnumSource(names = {
            "CONNECTION_RESET_BY_PEER",
            "RANDOM_DATA_THEN_CLOSE",
            "EMPTY_RESPONSE"
    })
    void should_enqueue_for_retry_on_fault(Fault fault) {
        givenThat(post(CALLBACK_URL)
                .willReturn(aResponse()
                        .withFault(fault)));

        sendAttempter.attemptSend(enqueuedItem);

        Mockito.verify(mockWebhookDeliveryQueueDao)
                .enqueueFrom(
                        any(),
                        eq(DeliveryStatus.PENDING),
                        argThat(instant -> instant.isAfter(Instant.parse(NOW))));
    }

    private static WebhookDeliveryQueueEntity buildWebhookDeliveryQueueEntityWithCallbackURL(String callbackUrl) {
        var webhook = aWebhookEntity()
                .withCallbackUrl(callbackUrl)
                .buld();
        var webhookMessage = aWebhookMessageEntity()
                .withWebhookEntity(webhook)
                .build();
        return aWebhookDeliveryQueueEntity()
                .withWebhookMessageEntity(webhookMessage)
                .build();
    }

    private SendAttempter buildSendAttempter() {
        var instantSource = InstantSource.fixed(Instant.parse(NOW));
        var webhookMessageSender = buildWebhookMessageSender();
        var environment = new Environment("fake-environment");
        return new SendAttempter(
                mockWebhookDeliveryQueueDao,
                instantSource,
                webhookMessageSender,
                environment
        );
    }

    private static WebhookMessageSender buildWebhookMessageSender() {
        var httpClient = HttpClients.createDefault();
        var httpPostFactory = new HttpPostFactory();
        var objectMapper = new ObjectMapper();
        var callbackUrlService = mock(CallbackUrlService.class);
        var webhookMessageSignatureGenerator = new WebhookMessageSignatureGenerator();
        return new WebhookMessageSender(
                httpClient,
                httpPostFactory,
                objectMapper,
                callbackUrlService,
                webhookMessageSignatureGenerator
        );
    }
}

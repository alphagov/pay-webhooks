package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.dropwizard.core.setup.Environment;
import io.github.netmikey.logunit.api.LogCapturer;
import org.apache.http.impl.client.HttpClients;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.message.HttpPostFactory;
import uk.gov.pay.webhooks.message.WebhookMessageSender;
import uk.gov.pay.webhooks.message.WebhookMessageSignatureGenerator;
import uk.gov.pay.webhooks.validations.CallbackUrlService;

import java.time.Instant;
import java.time.InstantSource;

import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.givenThat;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.slf4j.Logger.ROOT_LOGGER_NAME;
import static uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntityFixture.aWebhookDeliveryQueueEntity;
import static uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntityFixture.aWebhookMessageEntity;
import static uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntityFixture.aWebhookEntity;

@WireMockTest
class SendAttempterIT {

    @RegisterExtension
    LogCapturer logs = LogCapturer.create().captureForLogger(ROOT_LOGGER_NAME);

    private static final String CALLBACK_URL = "/callback-url";
    private static final String NOW = "2022-01-01T00:00:00.000Z";
    private SendAttempter sendAttempter;

    @BeforeEach
    void setUp() {
        sendAttempter = buildSendAttempter();
    }

    @Nested
    class when_send_successful {

        private WebhookDeliveryQueueEntity enqueuedItem;

        @BeforeEach
        void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
            givenThat(post(CALLBACK_URL)
                    .willReturn(ok()));
            enqueuedItem = buildWebhookDeliveryQueueEntityWithCallbackURL(wmRuntimeInfo.getHttpBaseUrl() + CALLBACK_URL);
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

            assertThat(logs.size())
                    .isEqualTo(2);
            logs.assertContains("Sending webhook message started");
            logs.assertContains("Sending webhook message finished");
        }
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

    private static @NonNull SendAttempter buildSendAttempter() {
        var instantSource = InstantSource.fixed(Instant.parse("2022-01-01T00:00:00Z"));
        var webhookMessageSender = buildWebhookMessageSender();
        var environment = new Environment("fake-environment");
        var webhookDeliveryQueueDao = mock(WebhookDeliveryQueueDao.class);
        return new SendAttempter(
                webhookDeliveryQueueDao,
                instantSource,
                webhookMessageSender,
                environment
        );
    }

    private static @NonNull WebhookMessageSender buildWebhookMessageSender() {
        var httpClient = HttpClients.createDefault();
        var httpPostFactory = new HttpPostFactory();
        var objectMapper = new ObjectMapper();
        var callbackUrlService = mock(CallbackUrlService.class); // Using Mockito since the WebhooksConfig has no constructor
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

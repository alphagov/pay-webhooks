package uk.gov.pay.webhooks.deliveryqueue;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.dropwizard.testing.ConfigOverride;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.rule.SqsTestDocker;
import uk.gov.pay.webhooks.ledger.LedgerStub;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;
import uk.gov.service.payments.commons.testing.port.PortFactory;

import java.io.IOException;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.gov.pay.webhooks.ledger.TransactionFromLedgerFixture.aTransactionFromLedgerFixture;
import static uk.gov.pay.webhooks.util.SNSToSQSEventFixture.anSNSToSQSEventFixture;


public class WebhookDeliveryQueueIT {
    private static final int webhookCallbackEndpointStubPort = PortFactory.findFreePort();
    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension(
            ConfigOverride.config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true"),
            ConfigOverride.config("webhookMessageSendingQueueProcessorConfig.initialDelayInMilliseconds", "0"),
            ConfigOverride.config("webhookMessageSendingQueueProcessorConfig.threadDelayInMilliseconds", "10")
    );
    @RegisterExtension
    public static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(app.getWireMockPort()).httpsPort(webhookCallbackEndpointStubPort))
            .build();
    private DatabaseTestHelper dbHelper;
    private final LedgerStub ledgerStub = new LedgerStub(wireMock);

    @BeforeEach
    public void setUp() {
        dbHelper = DatabaseTestHelper.aDatabaseTestHelper(app.getJdbi());
        dbHelper.truncateAllData();
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryStatus.class)
    public void deliveryStatusEnumIsConsistentWithDatabase(DeliveryStatus status) {
        app.getJdbi().withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', 'webhook-external-id', 'signing-key', 'service-id', true, 'https://callback-url.test', 'description', 'ACTIVE')"));
        app.getJdbi().withHandle(h -> h.execute("INSERT INTO webhook_messages VALUES (1, 'message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', 'transaction-external-id', 'payment')"));
        assertDoesNotThrow(() -> app.getJdbi().withHandle(h -> h.execute("INSERT INTO webhook_delivery_queue VALUES (1, '2022-01-01', '2022-01-01', '200', 200, 1, '%s', 1250)".formatted(status))));
    }

    @Test
    public void webhookMessageIsEmittedForSubscribedWebhook() throws IOException, InterruptedException {
        var serviceExternalId = "a-valid-service-id";
        dbHelper.addWebhookWithSubscription("a-valid-webhook-id", serviceExternalId, "http://localhost:%d/a-test-endpoint".formatted(app.getWireMockPort()));
        
        var transaction = aTransactionFromLedgerFixture();
        var sqsMessage = anSNSToSQSEventFixture()
                .withBody(Map.of(
                        "service_id", serviceExternalId,
                        "live", false,
                        "resource_external_id", transaction.getTransactionId(),
                        "timestamp", "2023-03-14T09:00:00.000000Z",
                        "resource_type", "payment",
                        "event_type", "USER_APPROVED_FOR_CAPTURE",
                        "sqs_message_id", "dc142884-1e4b-4e57-be93-111b692a4868"
                ));

        ledgerStub.returnLedgerTransaction(transaction);
        wireMock.stubFor(post("/a-test-endpoint").willReturn(ResponseDefinitionBuilder.okForJson("{}")));

        app.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), sqsMessage.build());
        Thread.sleep(1000);

        wireMock.verify(
                exactly(1),
                postRequestedFor(urlEqualTo("/a-test-endpoint")).withRequestBody(matchingJsonPath("$.resource_id", equalTo(transaction.getTransactionId())))
        ); 
    }
}

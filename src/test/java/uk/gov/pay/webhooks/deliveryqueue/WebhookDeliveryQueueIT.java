package uk.gov.pay.webhooks.deliveryqueue;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
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
import static io.dropwizard.testing.ConfigOverride.config;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static uk.gov.pay.webhooks.ledger.TransactionFromLedgerFixture.aTransactionFromLedgerFixture;
import static uk.gov.pay.webhooks.util.SNSToSQSEventFixture.anSNSToSQSEventFixture;

public class WebhookDeliveryQueueIT {
    private static final int webhookCallbackEndpointStubPort = PortFactory.findFreePort();
    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension(config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true"));
    @RegisterExtension
    public static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(app.getWireMockPort()).httpsPort(webhookCallbackEndpointStubPort))
            .build();
    private DatabaseTestHelper dbHelper;
    private final LedgerStub ledgerStub = new LedgerStub(wireMock);

    @BeforeEach
    public void setUp() {
        dbHelper = DatabaseTestHelper.aDatabaseTestHelper(app.getJdbi());
        dbHelper.truncateAllWebhooksData();
    }

    @Test
    public void webhookMessageIsEmittedForSubscribedWebhook() throws IOException, InterruptedException {
        var serviceExternalId = "a-valid-service-id";
        var gatewayAccountId = "100";
        DatabaseTestHelper.Webhook webhook = new DatabaseTestHelper.Webhook(
                1,
                "a-valid-webhook-id",
                serviceExternalId,
                "http://localhost:%d/a-test-endpoint".formatted(app.getWireMockPort()),
                "false",
                gatewayAccountId);
        dbHelper.addWebhook(webhook);
        DatabaseTestHelper.WebhookSubscription webhookSubscription1 = new DatabaseTestHelper.WebhookSubscription(
                1, "card_payment_succeeded");
        dbHelper.addWebhookSubscription(webhookSubscription1);
        DatabaseTestHelper.WebhookSubscription webhookSubscription2 = new DatabaseTestHelper.WebhookSubscription(
                1, "card_payment_refunded");
        dbHelper.addWebhookSubscription(webhookSubscription2);

        var transaction = aTransactionFromLedgerFixture();
        var sqsMessage = anSNSToSQSEventFixture()
                .withBody(Map.of(
                        "service_id", serviceExternalId,
                        "gateway_account_id", gatewayAccountId,
                        "live", false,
                        "resource_external_id", transaction.getTransactionId(),
                        "timestamp", "2023-03-14T09:00:00.000000Z",
                        "resource_type", "payment",
                        "event_type", "USER_APPROVED_FOR_CAPTURE",
                        "sqs_message_id", "dc142884-1e4b-4e57-be93-111b692a4868"
                ));

        ledgerStub.returnLedgerTransaction(transaction);
        wireMock.stubFor(post("/a-test-endpoint").willReturn(ResponseDefinitionBuilder.okForJson("{}")));

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(SqsTestDocker.getQueueUrl("event-queue"))
                .messageBody(sqsMessage.build())
                .build();
        app.getSqsClient().sendMessage(sendMessageRequest);
        Thread.sleep(1000);

        wireMock.verify(
                exactly(1),
                postRequestedFor(urlEqualTo("/a-test-endpoint")).withRequestBody(matchingJsonPath("$.resource.payment_id", equalTo(transaction.getTransactionId())))
        );

        given().port(app.getAppRule().getLocalPort())
                .contentType(ContentType.JSON)
                .get("/v1/webhook/a-valid-webhook-id/message")
                .then()
                .body("results[0].latest_attempt.status", is("SUCCESSFUL"))
                .body("results[0].last_delivery_status", is("SUCCESSFUL"));
    }

    @Test
    public void webhookMessageIsEmittedForSubscribedWebhook_forChildPaymentEvents() throws IOException, InterruptedException {
        var serviceExternalId = "a-valid-service-id";
        var gatewayAccountId = "100";
        DatabaseTestHelper.Webhook webhook = new DatabaseTestHelper.Webhook(
                1,
                "a-valid-webhook-id",
                serviceExternalId,
                "http://localhost:%d/a-test-endpoint".formatted(app.getWireMockPort()),
                "false",
                gatewayAccountId);
        dbHelper.addWebhook(webhook);
        DatabaseTestHelper.WebhookSubscription webhookSubscription1 = new DatabaseTestHelper.WebhookSubscription(
                1, "card_payment_succeeded");
        dbHelper.addWebhookSubscription(webhookSubscription1);
        DatabaseTestHelper.WebhookSubscription webhookSubscription2 = new DatabaseTestHelper.WebhookSubscription(
                1, "card_payment_refunded");
        dbHelper.addWebhookSubscription(webhookSubscription2);
        var transaction = aTransactionFromLedgerFixture();
        var sqsMessage = anSNSToSQSEventFixture()
                .withBody(Map.of(
                        "service_id", serviceExternalId,
                        "gateway_account_id", gatewayAccountId,
                        "live", false,
                        "resource_external_id", "refund-external-id",
                        "parent_resource_external_id", transaction.getTransactionId(),
                        "timestamp", "2023-03-14T09:00:00.000000Z",
                        "resource_type", "refund",
                        "event_type", "REFUND_SUCCEEDED",
                        "sqs_message_id", "dc142884-1e4b-4e57-be93-111b692a4868"
                ));

        // the payment, rather than the child resource (refund) is requested from ledger
        ledgerStub.returnLedgerTransaction(transaction);
        wireMock.stubFor(post("/a-test-endpoint").willReturn(ResponseDefinitionBuilder.okForJson("{}")));

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(SqsTestDocker.getQueueUrl("event-queue"))
                .messageBody(sqsMessage.build())
                .build();
        app.getSqsClient().sendMessage(sendMessageRequest);
        Thread.sleep(1000);

        // resource body is appropriately formatted as a payment
        wireMock.verify(
                exactly(1),
                postRequestedFor(urlEqualTo("/a-test-endpoint")).withRequestBody(matchingJsonPath("$.resource.payment_id", equalTo(transaction.getTransactionId())))
        );
    }

    @Test
    public void webhookMessageLastDeliveryStatusIsConsistent() throws InterruptedException, IOException {
        var serviceExternalId = "a-valid-service-id";
        var gatewayAccountId = "100";

        DatabaseTestHelper.Webhook webhook1 = new DatabaseTestHelper.Webhook(
                1,
                "webhook-external-id-succeeds",
                serviceExternalId,
                "http://localhost:%d/a-working-endpoint".formatted(app.getWireMockPort()),
                "false",
                gatewayAccountId);
        dbHelper.addWebhook(webhook1);
        DatabaseTestHelper.Webhook webhook2 = new DatabaseTestHelper.Webhook(
                2,
                "webhook-external-id-fails",
                serviceExternalId,
                "http://localhost:%d/a-failing-endpoint".formatted(app.getWireMockPort()),
                "false",
                gatewayAccountId);
        dbHelper.addWebhook(webhook2);
        DatabaseTestHelper.WebhookSubscription webhookSubscription1 = new DatabaseTestHelper.WebhookSubscription(
                1, "card_payment_succeeded");
        dbHelper.addWebhookSubscription(webhookSubscription1);
        DatabaseTestHelper.WebhookSubscription webhookSubscription2 = new DatabaseTestHelper.WebhookSubscription(
                2, "card_payment_succeeded");
        dbHelper.addWebhookSubscription(webhookSubscription2);
        var transaction = aTransactionFromLedgerFixture();
        var sqsMessage = anSNSToSQSEventFixture()
                .withBody(Map.of(
                        "service_id", serviceExternalId,
                        "gateway_account_id", gatewayAccountId,
                        "live", false,
                        "resource_external_id", transaction.getTransactionId(),
                        "timestamp", "2023-03-14T09:00:00.000000Z",
                        "resource_type", "payment",
                        "event_type", "USER_APPROVED_FOR_CAPTURE",
                        "sqs_message_id", "dc142884-1e4b-4e57-be93-111b692a4868"
                ));

        ledgerStub.returnLedgerTransaction(transaction);
        wireMock.stubFor(post("/a-working-endpoint").willReturn(ResponseDefinitionBuilder.okForJson("{}")));
        wireMock.stubFor(post("/a-failing-endpoint").willReturn(WireMock.forbidden()));

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(SqsTestDocker.getQueueUrl("event-queue"))
                .messageBody(sqsMessage.build())
                .build();
        app.getSqsClient().sendMessage(sendMessageRequest);
        Thread.sleep(2000);

        wireMock.verify(
                exactly(1),
                postRequestedFor(urlEqualTo("/a-working-endpoint")).withRequestBody(matchingJsonPath("$.resource_id", equalTo(transaction.getTransactionId())))
        );
        wireMock.verify(
                exactly(1),
                postRequestedFor(urlEqualTo("/a-failing-endpoint")).withRequestBody(matchingJsonPath("$.resource_id", equalTo(transaction.getTransactionId())))
        );

        given().port(app.getAppRule().getLocalPort())
                .contentType(ContentType.JSON)
                .get("/v1/webhook/webhook-external-id-succeeds/message")
                .then()
                .body("results[0].latest_attempt.status", is("SUCCESSFUL"))
                .body("results[0].last_delivery_status", is("SUCCESSFUL"));
        given().port(app.getAppRule().getLocalPort())
                .contentType(ContentType.JSON)
                .get("/v1/webhook/webhook-external-id-fails/message")
                .then()
                .body("results[0].latest_attempt.status", is("FAILED"))
                .body("results[0].latest_attempt.status_code", is(403))
                .body("results[0].last_delivery_status", is("FAILED"));
    }
}

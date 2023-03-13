package uk.gov.pay.webhooks.deliveryqueue;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
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
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;
import uk.gov.pay.webhooks.ledger.model.TransactionState;
import uk.gov.pay.webhooks.queue.sqs.QueueException;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class WebhookDeliveryQueueIT {
    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension(
            ConfigOverride.config("queueMessageReceiverConfig.backgroundProcessingEnabled", "true"),
            ConfigOverride.config("webhookMessageSendingQueueProcessorConfig.initialDelayInMilliseconds", "0"),
            ConfigOverride.config("webhookMessageSendingQueueProcessorConfig.threadDelayInMilliseconds", "10")
    );
    @RegisterExtension
    public static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(app.getWireMockPort()))
            .build();
    private DatabaseTestHelper dbHelper;
    private LedgerStub ledgerStub = new LedgerStub(wireMock);

    @BeforeEach
    public void setUp() {
        dbHelper = DatabaseTestHelper.aDatabaseTestHelper(app.getJdbi());
        dbHelper.truncateAllData();
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryStatus.class)
    public void deliveryStatusEnumIsConsistentWithDatabase(DeliveryStatus status) {
        app.getJdbi().withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', 'webhook-external-id', 'signing-key', 'service-id', true, 'https://callback-url.gov.uk', 'description', 'ACTIVE')"));
        app.getJdbi().withHandle(h -> h.execute("INSERT INTO webhook_messages VALUES (1, 'message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', 'transaction-external-id', 'payment')"));
        assertDoesNotThrow(() -> app.getJdbi().withHandle(h -> h.execute("INSERT INTO webhook_delivery_queue VALUES (1, '2022-01-01', '2022-01-01', '200', 200, 1, '%s', 1250)".formatted(status))));
    }

    @Test
    public void testingEnvironmnet() throws InterruptedException, QueueException {
        app.getJdbi().withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', 'webhook-external-id', 'signing-key', 'some-service-id', false, 'http://localhost:" + app.getWireMockPort() + "/a-test-endpoint', 'description', 'ACTIVE')"));
        app.getJdbi().withHandle(h -> h.execute("INSERT INTO webhook_subscriptions VALUES (1, (SELECT id FROM event_types WHERE name = 'card_payment_succeeded'))"));

        // this could either come from the pact fixture or should have a default way of making it (LedgerTransactionFixture)
        var transaction = new LedgerTransaction();
        transaction.setTransactionId("t8cj9v1lci7da7pbp99qg9olv3");
        transaction.setState(new TransactionState("created"));
        transaction.setLive(true);
        transaction.setMoto(false);
        transaction.setDelayedCapture(false);
        transaction.setCreatedDate("2023-03-13T14:12:04.204Z");
        transaction.setCredentialExternalId("credential-external-id");
        transaction.setPaymentProvider("sandbox");
        transaction.setEmail("Joe.Bogs@example.org");
        transaction.setReturnUrl("https://service-name.gov.uk/transactions/12345");
        transaction.setReference("a-reference");
        transaction.setDescription("a-description");
        transaction.setGatewayAccountId("3");
        transaction.setAmount(1000L);
        ledgerStub.returnLedgerTransaction("t8cj9v1lci7da7pbp99qg9olv3", transaction);
        wireMock.stubFor(post("/a-test-endpoint").willReturn(ResponseDefinitionBuilder.okForJson("{}")));
        var sqsMessage = """
                {
                  "Type" : "Notification",
                  "MessageId" : "04424f78-d540-5eb7-87e1-154d586b6b02",
                  "Message" : "{\\"sqs_message_id\\":\\"dc142884-1e4b-4e57-be93-111b692a4868\\",\\"service_id\\":\\"some-service-id\\",\\"live\\":false,\\"resource_type\\":\\"payment\\",\\"resource_external_id\\":\\"t8cj9v1lci7da7pbp99qg9olv3\\",\\"parent_resource_external_id\\":null,\\"timestamp\\":\\"2019-08-31T14:18:46.446541Z\\",\\"event_type\\":\\"USER_APPROVED_FOR_CAPTURE\\",\\"reproject_domain_object\\":false}",
                  "TopicArn" : "card-payment-events-topic",
                  "Timestamp" : "2021-12-16T18:52:27.068Z",
                  "SignatureVersion" : "1",
                  "Signature" : "some-signature",
                  "SigningCertURL" : "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-signing-cert-uuid.pem",
                  "UnsubscribeURL" : "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:a-aws-arn"
                }
                """;
        app.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), sqsMessage);

        Thread.sleep(300);
        wireMock.verify(exactly(1), postRequestedFor(urlEqualTo("/a-test-endpoint")));
    }
}

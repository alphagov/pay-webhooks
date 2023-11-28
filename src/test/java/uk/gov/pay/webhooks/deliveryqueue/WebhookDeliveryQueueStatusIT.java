package uk.gov.pay.webhooks.deliveryqueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class WebhookDeliveryQueueStatusIT {
    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension();
    private DatabaseTestHelper dbHelper;

    @BeforeEach
    public void setUp() {
        dbHelper = DatabaseTestHelper.aDatabaseTestHelper(app.getJdbi());
        dbHelper.truncateAllWebhooksData();
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryStatus.class)
    public void deliveryStatusEnumIsConsistentWithDatabase(DeliveryStatus status) {
        DatabaseTestHelper.Webhook webhook = DatabaseTestHelper.Webhook.builder()
                .webhookId(1)
                .webhookExternalId("webhook-external-id")
                .serviceExternalId("service-id")
                .endpointUrl("https://callback-url.test")
                .live("true")
                .gatewayAccountId("100")
                .build();
        dbHelper.addWebhook(webhook);
        DatabaseTestHelper.WebhookMessage webhookMessage = DatabaseTestHelper.WebhookMessage.builder()
                .webhookMessageId(1)
                .externalId("message-external-id")
                .createdDate("2022-01-01")
                .webhookId(1)
                .eventDate("2022-01-01")
                .eventType(1)
                .resource("{}")
                .resourceExternalId("transaction-external-id")
                .resourceType("payment")
                .deliveryStatus(status).build();
        dbHelper.addWebhookMessage(webhookMessage);
        DatabaseTestHelper.WebhookDeliveryQueueMessage webhookDeliveryQueueMessage = DatabaseTestHelper.WebhookDeliveryQueueMessage.builder()
                .deliveryQueueMessageId(1)
                .sentDate("2022-01-01")
                .createdDate("2022-01-01")
                .deliveryResult("200")
                .statusCode(200)
                .webhookMessageId(1)
                .deliveryStatus(status)
                .deliveryCode(1250).build();
        assertDoesNotThrow(() -> dbHelper.addWebhookDeliveryQueueMessage(webhookDeliveryQueueMessage));
    }
}

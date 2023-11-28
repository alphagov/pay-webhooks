package uk.gov.pay.webhooks.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class WebhookMessageIT {
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
    public void deliveryStatusEnumIsConsistentWithWebhookMessageLastDeliveryStatus(DeliveryStatus status) {
        DatabaseTestHelper.Webhook webhook = new DatabaseTestHelper.Webhook(
                1,
                "webhook-external-id",
                "service-id",
                "https://callback-url.test",
                "true",
                "100");
        dbHelper.addWebhook(webhook);
        DatabaseTestHelper.WebhookMessage webhookMessage = new DatabaseTestHelper.WebhookMessage(
                1,
                "message-external-id",
                "2022-01-01",
                1,
                "2022-01-01",
                1,
                "{}",
                "transaction-external-id",
                "payment",
                status);
        assertDoesNotThrow(() -> dbHelper.addWebhookMessage(webhookMessage));
    }
}

package uk.gov.pay.webhooks.deliveryqueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

import java.util.Optional;

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
        dbHelper.addWebhook();
        dbHelper.addWebhookMessage(1,"message-external-id", "2022-01-01", 1, "2022-01-01", 1, "{}", "transaction-external-id", "payment",status);
        assertDoesNotThrow(() -> dbHelper.addWebhookDeliveryQueueMessage(1, "2022-01-01", "2022-01-01", "200", 200, 1, status, 1250));
    }
}

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
        dbHelper.truncateAllData();
    }

    @ParameterizedTest
    @EnumSource(value = DeliveryStatus.class)
    public void deliveryStatusEnumIsConsistentWithDatabase(DeliveryStatus status) {
        dbHelper.addDeliveryStatusEnumIsConsistentWithDatabase();
        assertDoesNotThrow(() -> dbHelper.addDeliveryStatusEnumIsConsistentWithDatabaseWebHookDeliveryQueueInsert(status));
    }
}

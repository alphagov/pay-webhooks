package uk.gov.pay.webhooks.deliveryqueue.managed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

class WebhookMessagePollingServiceIT {
    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension();

    private DatabaseTestHelper dbHelper;

    @BeforeEach
    public void setUp() {
        dbHelper = DatabaseTestHelper.aDatabaseTestHelper(app.getJdbi());
        dbHelper.truncateAllData();
    }

    // @TOOD(sfount) propose IT joins the dots between DB and HTTP with real postgres and wiremock
    //               just the happy path should be fine
    @Test
    public void shouldPollAndGetNothingForEmptyQueue() {
    }

    @Test
    public void shouldPollAndEmit() {
    }
}

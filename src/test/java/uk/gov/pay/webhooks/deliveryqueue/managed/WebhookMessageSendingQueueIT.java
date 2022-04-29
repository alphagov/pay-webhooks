package uk.gov.pay.webhooks.deliveryqueue.managed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

class WebhookMessageSendingQueueIT {
    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension();
    private Integer port = app.getAppRule().getLocalPort();
    private DatabaseTestHelper dbHelper;



    @BeforeEach
    public void setUp() {
        dbHelper = DatabaseTestHelper.aDatabaseTestHelper(app.getJdbi());
        dbHelper.truncateAllData();
    }

    @Test
    void processQueueShouldProcessMultipleItems () throws InterruptedException {
        dbHelper.createWebhook();
        dbHelper.createMessage();
        dbHelper.addMessageToQueue();
//        Delay to allow the queue processor to run
        Thread.sleep(10000);
//        TODO: investigate why we get error: uk.gov.pay.webhooks.deliveryqueue.managed.WebhookMessageSendingQueueProcessor: Unexpected exception when attempting to send
//         org.hibernate.HibernateException: No session currently bound to execution context
        
        
    }
}

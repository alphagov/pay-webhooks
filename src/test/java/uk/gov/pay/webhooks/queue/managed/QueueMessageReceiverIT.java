package uk.gov.pay.webhooks.queue.managed;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresExtension;
import uk.gov.pay.rule.SqsTestDocker;

import java.time.ZonedDateTime;

@ExtendWith(DropwizardExtensionsSupport.class)
public class QueueMessageReceiverIT {

    @RegisterExtension
    public static AppWithPostgresExtension app = new AppWithPostgresExtension();
    private Integer port = app.getAppRule().getLocalPort();


    private static final ZonedDateTime CREATED_AT = ZonedDateTime.parse("2019-06-07T08:46:01.123456Z");

    @Test
    public void shouldHandleEvents() throws InterruptedException {
   
        var message = """
                {
                  "service_id": "abc",
                  "event_type": "CAPTURE_CONFIRMED",
                  "event_details": { "bob": "bob"},
                  "live": true
                }
                """;
        
        app.getSqsClient().sendMessage(SqsTestDocker.getQueueUrl("event-queue"), message);

        Thread.sleep(5000);
        
    }

}

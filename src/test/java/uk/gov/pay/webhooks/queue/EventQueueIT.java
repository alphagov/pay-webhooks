package uk.gov.pay.webhooks.queue;

import com.amazonaws.services.sqs.AmazonSQS;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.rule.SqsTestDocker;
import uk.gov.pay.webhooks.app.QueueMessageReceiverConfig;
import uk.gov.pay.webhooks.app.SqsConfig;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.queue.sqs.QueueException;
import uk.gov.pay.webhooks.queue.sqs.QueueMessage;
import uk.gov.pay.webhooks.queue.sqs.SqsQueueService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventQueueIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();
    private AmazonSQS client;

    @BeforeEach
    public void setUp() {
        client = rule.getSqsClient();
    }

    @Test
    public void shouldReceiveMessageFromTheQueue() throws QueueException {
        client.sendMessage(SqsTestDocker.getQueueUrl("event-queue"), "");

        SqsConfig sqsConfig = mock(SqsConfig.class);
        when(sqsConfig.getMessageMaximumBatchSize()).thenReturn(10);
        when(sqsConfig.getMessageMaximumWaitTimeInSeconds()).thenReturn(1);
        WebhooksConfig mockConfig = mock(WebhooksConfig.class);
        when(mockConfig.getSqsConfig()).thenReturn(sqsConfig);

        SqsQueueService sqsQueueService = new SqsQueueService(client, mockConfig);

        List<QueueMessage> result = sqsQueueService.receiveMessages(SqsTestDocker.getQueueUrl("event-queue"), "All");
        assertFalse(result.isEmpty());
        
    }
    
    @Test
    public void shouldConvertValidMessageFromQueueToEventMessage() throws QueueException {
        
        var sqsMessage = """
                {
                	"serviceId": "my-id",
                	"live": true,
                	"timestamp": "2018-03-12T16:25:01.123456Z",
                	"resource_external_id": "3uwuyr38rry",
                	"eventType": "PAYMENT_CREATED",
                	"event_details": {
                		"example_event_details_field": "and its value"
                	}
                }""";
        client.sendMessage(SqsTestDocker.getQueueUrl("event-queue"), sqsMessage);

        SqsConfig sqsConfig = mock(SqsConfig.class);
        when(sqsConfig.getMessageMaximumBatchSize()).thenReturn(10);
        when(sqsConfig.getMessageMaximumWaitTimeInSeconds()).thenReturn(1);
        when(sqsConfig.getEventQueueUrl()).thenReturn(SqsTestDocker.getQueueUrl("event-queue"));
        QueueMessageReceiverConfig queueReceiverConfig = mock(QueueMessageReceiverConfig.class);
        when(queueReceiverConfig.getMessageRetryDelayInSeconds()).thenReturn(10);
        WebhooksConfig mockConfig = mock(WebhooksConfig.class);
        when(mockConfig.getSqsConfig()).thenReturn(sqsConfig);
        when(mockConfig.getQueueMessageReceiverConfig()).thenReturn(queueReceiverConfig);

        SqsQueueService sqsQueueService = new SqsQueueService(client, mockConfig);
        EventQueue eventQueue = new EventQueue(sqsQueueService, mockConfig, new ObjectMapper());

        List<EventMessage> result = eventQueue.retrieveEvents();
        assertFalse(result.isEmpty());
    }
}

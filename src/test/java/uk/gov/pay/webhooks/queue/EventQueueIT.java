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
                  "Type" : "Notification",
                  "MessageId" : "04424f78-d540-5eb7-87e1-154d586b6b02",
                  "Message" : "{\\"sqs_message_id\\":\\"dc142884-1e4b-4e57-be93-111b692a4868\\",\\"service_id\\":\\"some-service-id\\",\\"live\\":false,\\"resource_type\\":\\"payment\\",\\"resource_external_id\\":\\"t8cj9v1lci7da7pbp99qg9olv3\\",\\"parent_resource_external_id\\":null,\\"event_date\\":\\"2019-08-31T14:18:46.446541Z\\",\\"event_type\\":\\"PAYMENT_DETAILS_ENTERED\\",\\"event_data\\":{},\\"reproject_domain_object\\":false}",
                  "TopicArn" : "card-payment-events-topic",
                  "Timestamp" : "2021-12-16T18:52:27.068Z",
                  "SignatureVersion" : "1",
                  "Signature" : "qYWtXrARHosfc5wgMSJLKofdn2q+QJIEs+XfpIBCFp94VHOujQBjVRGpQkr8OVF07lONWakkuKvdzcRIwRExtuCNDVyJinJGJgQEAFKpKmnJ9TfBsraTI5cmWAxnHv/wyYkK948QNe3DkdmascRU6ldKSIaZJ2k46cJfjtIkspMZ2tOuen39bd5pASXrGLyi9eq8HsZNY1IhD5OqDBtl+eLZ5DtJNdbroYF6+lg0f9K40fZIiPhFBtJPQoNZjCIwEb3VCG4o+OF7ga1gEOAj0se5BUXiMYWWT2zfusFBoxoecA/nout33sFS4NxGAsoKEkQlqxChSK1Lr/XTvZ77SA==",
                  "SigningCertURL" : "https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-signing-cert-uuid.pem",
                  "UnsubscribeURL" : "https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:a-aws-arn"
                }
                """;
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

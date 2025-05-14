package uk.gov.pay.webhooks.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.rule.SqsTestDocker;
import uk.gov.pay.webhooks.app.QueueMessageReceiverConfig;
import uk.gov.pay.webhooks.app.SqsConfig;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.queue.sqs.QueueException;
import uk.gov.pay.webhooks.queue.sqs.QueueMessage;
import uk.gov.pay.webhooks.queue.sqs.SqsQueueService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.pay.webhooks.util.SNSToSQSEventFixture.anSNSToSQSEventFixture;

class EventQueueIT {

    @RegisterExtension
    public static AppWithPostgresAndSqsExtension rule = new AppWithPostgresAndSqsExtension();
    private SqsClient client;

    @BeforeEach
    public void setUp() {
        client = rule.getSqsClient();
    }

    @Test
    void shouldReceiveMessageFromTheQueue() throws QueueException {
        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(SqsTestDocker.getQueueUrl("event-queue"))
                .messageBody("{ messageBody: \"example message\" }")
                .build();
        client.sendMessage(sendMessageRequest);

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
    void shouldConvertValidMessageFromQueueToEventMessage() throws QueueException, IOException {
        var eventMessage = Map.of(
                "sqs_message_id", "dc142884-1e4b-4e57-be93-111b692a4868",
                "service_id", "some-service-id",
                "gateway_account_id", "100",
                "live", "false",
                "resource_type", "payment",
                "resource_external_id", "t8cj9v1lci7da7pbp99qg9olv3",
                "timestamp", "2019-08-31T14:18:46.446541Z",
                "event_type", "PAYMENT_DETAILS_ENTERED"
        );
        var sqsMessage = anSNSToSQSEventFixture()
                .withBody(eventMessage)
                .build();

        SendMessageRequest sendMessageRequest = SendMessageRequest.builder()
                .queueUrl(SqsTestDocker.getQueueUrl("event-queue"))
                .messageBody(sqsMessage)
                .build();
        
        client.sendMessage(sendMessageRequest);

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
        assertThat(result.get(0).eventMessageDto().resourceType(), is("payment"));
        assertThat(result.get(0).eventMessageDto().gatewayAccountId(), is("100"));
    }
}

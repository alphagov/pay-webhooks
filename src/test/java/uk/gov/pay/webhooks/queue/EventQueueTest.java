package uk.gov.pay.webhooks.queue;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.app.SqsConfig;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.queue.sqs.QueueException;
import uk.gov.pay.webhooks.queue.sqs.QueueMessage;
import uk.gov.pay.webhooks.queue.sqs.SqsQueueService;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EventQueueTest {
    @Mock
    private Appender<ILoggingEvent> mockAppender;
    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventArgumentCaptor;
    @Mock
    private SqsQueueService sqsQueueService;
    @Mock
    private SqsConfig sqsConfig;
    @Mock
    private WebhooksConfig webhooksConfig;
    private EventQueue eventQueue;

    @BeforeEach
    public void setUp() {
        when(sqsConfig.getEventQueueUrl()).thenReturn("sqs://queue-url.test");
        when(webhooksConfig.getSqsConfig()).thenReturn(sqsConfig);
        eventQueue = new EventQueue(sqsQueueService, webhooksConfig, new ObjectMapper());
    }

    @Test
    public void shouldFilterEventsWithoutValidPropertiesAndLog() throws QueueException {
        Logger root = (Logger) LoggerFactory.getLogger(EventQueue.class);
        root.addAppender(mockAppender);

        var sqsMessageWithMissingServiceId = """
                {
                  "Message" : "{\\"sqs_message_id\\":\\"dc142884-1e4b-4e57-be93-111b692a4868\\",\\"live\\":false,\\"resource_type\\":\\"payment\\",\\"resource_external_id\\":\\"t8cj9v1lci7da7pbp99qg9olv3\\",\\"parent_resource_external_id\\":null,\\"timestamp\\":\\"2019-08-31T14:18:46.446541Z\\",\\"event_type\\":\\"PAYMENT_DETAILS_ENTERED\\",\\"reproject_domain_object\\":false}"
                }
                """;
        var sqsMessageWithMissingLiveProperty = """
                {
                  "Message" : "{\\"sqs_message_id\\":\\"dc142884-1e4b-4e57-be93-111b692a4868\\",\\"service_id\\":\\"some-service-id\\",\\"resource_type\\":\\"payment\\",\\"resource_external_id\\":\\"t8cj9v1lci7da7pbp99qg9olv3\\",\\"parent_resource_external_id\\":null,\\"timestamp\\":\\"2019-08-31T14:18:46.446541Z\\",\\"event_type\\":\\"PAYMENT_DETAILS_ENTERED\\",\\"reproject_domain_object\\":false}"
                }
                """;
        var sqsMessageWithValidProperties = """
                {
                  "Message" : "{\\"sqs_message_id\\":\\"dc142884-1e4b-4e57-be93-111b692a4868\\",\\"service_id\\":\\"some-service-id\\",\\"live\\":false,\\"resource_type\\":\\"payment\\",\\"resource_external_id\\":\\"t8cj9v1lci7da7pbp99qg9olv3\\",\\"parent_resource_external_id\\":null,\\"timestamp\\":\\"2019-08-31T14:18:46.446541Z\\",\\"event_type\\":\\"PAYMENT_DETAILS_ENTERED\\",\\"reproject_domain_object\\":false}"
                }
                """;
        var queueMessageWithMissingServiceId = new QueueMessage("message-id-missing-service-id", "some-receipt-handle", sqsMessageWithMissingServiceId);
        var queueMessageWithMissingLiveProperty = new QueueMessage("message-id-missing-live-property", "some-receipt-handle", sqsMessageWithMissingLiveProperty);
        var queueMessageWithValidProperties = new QueueMessage("message-id-valid-properties", "some-receipt-handle", sqsMessageWithValidProperties);
        when(sqsQueueService.receiveMessages("sqs://queue-url.test", "All")).thenReturn(List.of(queueMessageWithMissingServiceId, queueMessageWithMissingLiveProperty, queueMessageWithValidProperties));

        var results = eventQueue.retrieveEvents();

        verify(mockAppender, times(2)).doAppend(loggingEventArgumentCaptor.capture());
        var logs = loggingEventArgumentCaptor.getAllValues();
        assertThat(logs.get(0).getMessage(), containsString("Unable to process events without `service_id` or `live` properties"));
        assertThat(logs.get(1).getMessage(), containsString("Unable to process events without `service_id` or `live` properties"));
        assertThat(results.size(), is(1));
        assertThat(results.get(0).queueMessage().messageId(), is("message-id-valid-properties"));
    }

}

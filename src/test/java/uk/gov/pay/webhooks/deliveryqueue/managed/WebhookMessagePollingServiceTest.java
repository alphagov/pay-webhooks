package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.WebhookMessageSender;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookMessagePollingServiceTest {

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(EventTypeEntity.class)
            .addEntityClass(WebhookMessageEntity.class)
            .addEntityClass(WebhookEntity.class)
            .addEntityClass(WebhookDeliveryQueueEntity.class)
            .build();

    private InstantSource instantSource;
    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private WebhookMessageDao webhookMessageDao;
    private WebhookDao webhookDao;
    private WebhookMessagePollingService webhookMessagePollingService;
    private WebhookMessageSender webhookMessageSenderMock;
    private SendAttempter sendAttempter;
    @Mock
    private HttpResponse response;

    @BeforeEach
    public void setUp() throws IOException, InvalidKeyException, InterruptedException {
        var environment = mock(Environment.class);

        when(environment.metrics()).thenReturn(mock(MetricRegistry.class));
        when(response.statusCode()).thenReturn(200);
        webhookMessageSenderMock = mock(WebhookMessageSender.class);
        when(webhookMessageSenderMock.sendWebhookMessage(any())).thenReturn(response);
        instantSource = InstantSource.fixed(Instant.now());
        webhookMessageDao = new WebhookMessageDao(database.getSessionFactory());
        webhookDao = new WebhookDao(database.getSessionFactory());
        webhookDeliveryQueueDao = new WebhookDeliveryQueueDao(database.getSessionFactory(), instantSource);

        sendAttempter = new SendAttempter(webhookDeliveryQueueDao, instantSource, webhookMessageSenderMock, environment);
        webhookMessagePollingService = new WebhookMessagePollingService(webhookDeliveryQueueDao, sendAttempter, database.getSessionFactory());
    }

    @Test
    public void shouldPollWithNoActionsOnEmptyQueue() {
       webhookMessagePollingService.pollWebhookMessageQueue();
       verifyNoInteractions(webhookMessageSenderMock);
    }

    @Test
    public void shouldEmitSingleValidQueueItem() throws IOException, InvalidKeyException, InterruptedException {
        setupOrderedDeliveryQueueWith(List.of("first-external-id"));
        webhookMessagePollingService.pollWebhookMessageQueue();
        verify(webhookMessageSenderMock).sendWebhookMessage(argThat((webhookMessage -> webhookMessage.getExternalId().equals("first-external-id"))));
    }

    @Test
    public void shouldEmitMultipleValidQueueItemsInOnePoll() throws IOException, InvalidKeyException, InterruptedException {
        setupOrderedDeliveryQueueWith(List.of("first-external-id", "second-external-id"));
        webhookMessagePollingService.pollWebhookMessageQueue();

        var captor = ArgumentCaptor.forClass(WebhookMessageEntity.class);
        verify(webhookMessageSenderMock, times(2)).sendWebhookMessage(captor.capture());
        assertEquals("first-external-id", captor.getAllValues().get(0).getExternalId());
        assertEquals("second-external-id", captor.getAllValues().get(1).getExternalId());
    }

    @Test
    public void shouldAppropriatelyHandleFailedEmitAndNotTryAgainImmediately() throws IOException, InvalidKeyException, InterruptedException {
        when(response.statusCode()).thenReturn(500);

        setupOrderedDeliveryQueueWith(List.of("first-external-id"));
        webhookMessagePollingService.pollWebhookMessageQueue();
        verify(webhookMessageSenderMock).sendWebhookMessage(argThat((webhookMessage -> webhookMessage.getExternalId().equals("first-external-id"))));

        webhookMessagePollingService.pollWebhookMessageQueue();
        verifyNoMoreInteractions(webhookMessageSenderMock);
    }

    private void setupOrderedDeliveryQueueWith(List<String> messageIds) {
        WebhookEntity webhookEntity = new WebhookEntity();

        database.inTransaction(() -> {
            webhookEntity.setCallbackUrl("http://a-callback-url.com");
            webhookEntity.setServiceId("a-service-id");
            webhookEntity.setLive(false);

            webhookDao.create(webhookEntity);
        });

        for (int i = 0; i < messageIds.size(); i++) {
            int finalI = i;

            var message = database.inTransaction(() -> {
                WebhookMessageEntity webhookMessageEntity = new WebhookMessageEntity();
                var id = messageIds.get(finalI);
                webhookMessageEntity.setExternalId(id);
                webhookMessageEntity.setWebhookEntity(webhookEntity);
                webhookMessageEntity.setCreatedDate(instantSource.instant());

                return webhookMessageDao.create(webhookMessageEntity);
            });
            database.inTransaction(() -> {
                webhookDeliveryQueueDao.enqueueFrom(message, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant().minusMillis((messageIds.size() - finalI) * 10L));
            });
        }
    }
}

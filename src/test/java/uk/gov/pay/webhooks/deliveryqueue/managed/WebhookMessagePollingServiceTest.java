package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
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
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeDao;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.WebhookMessageSender;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WebhookMessagePollingServiceTest {

    private final DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(EventTypeEntity.class)
            .addEntityClass(WebhookMessageEntity.class)
            .addEntityClass(WebhookEntity.class)
            .addEntityClass(WebhookDeliveryQueueEntity.class)
            .addEntityClass(EventTypeEntity.class)
            .build();

    private InstantSource instantSource;
    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private WebhookMessageDao webhookMessageDao;
    private WebhookDao webhookDao;
    private EventTypeDao eventTypeDao;
    private WebhookMessagePollingService webhookMessagePollingService;
    private WebhookMessageSender webhookMessageSenderMock;
    private SendAttempter sendAttempter;
    @Mock
    private HttpResponse response;
    @Mock
    private StatusLine statusLine;

    @BeforeEach
    void setUp() throws IOException, InvalidKeyException, InterruptedException {
        var environment = mock(Environment.class);

        when(environment.metrics()).thenReturn(mock(MetricRegistry.class));
        when(response.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(200);
        webhookMessageSenderMock = mock(WebhookMessageSender.class);
        when(webhookMessageSenderMock.sendWebhookMessage(any(WebhookMessageEntity.class))).thenReturn(response);
        instantSource = InstantSource.fixed(Instant.now());
        webhookMessageDao = new WebhookMessageDao(database.getSessionFactory());
        webhookDao = new WebhookDao(database.getSessionFactory());
        eventTypeDao = new EventTypeDao(database.getSessionFactory());
        webhookDeliveryQueueDao = new WebhookDeliveryQueueDao(database.getSessionFactory(), instantSource);
        sendAttempter = new SendAttempter(webhookDeliveryQueueDao, instantSource, webhookMessageSenderMock, environment);
        webhookMessagePollingService = new UnitOfWorkAwareProxyFactory("default", database.getSessionFactory())
                .create(
                        WebhookMessagePollingService.class,
                        new Class[] { WebhookDeliveryQueueDao.class, SendAttempter.class },
                        new Object[] { webhookDeliveryQueueDao, sendAttempter }
                );
    }

    @Test
    public void shouldPollWithNoActionsOnEmptyQueue() {
        database.inTransaction(() -> {
            webhookMessagePollingService.pollWebhookMessageQueue();
            verifyNoInteractions(webhookMessageSenderMock);
        });
    }

    @Test
    public void shouldEmitSingleValidQueueItem() {
        setupOrderedDeliveryQueueWith(List.of("first-external-id"));
        database.inTransaction(() -> {
            webhookMessagePollingService.pollWebhookMessageQueue();
            try {
                verify(webhookMessageSenderMock).sendWebhookMessage(argThat((webhookMessage -> webhookMessage.getExternalId().equals("first-external-id"))));
            } catch (IOException | InvalidKeyException | InterruptedException ignored) { fail(); }
        });
    }

    @Test
    public void shouldEmitMultipleValidQueueItemsInOnePoll() {
        setupOrderedDeliveryQueueWith(List.of("first-external-id", "second-external-id"));
        database.inTransaction(() -> {
            webhookMessagePollingService.pollWebhookMessageQueue();

            var captor = ArgumentCaptor.forClass(WebhookMessageEntity.class);
            try {
            verify(webhookMessageSenderMock, times(2)).sendWebhookMessage(captor.capture());
            } catch (IOException | InvalidKeyException | InterruptedException ignored) { fail(); }
            assertEquals("first-external-id", captor.getAllValues().get(0).getExternalId());
            assertEquals("second-external-id", captor.getAllValues().get(1).getExternalId());
        });
    }

    @Test
    public void shouldAppropriatelyHandleFailedEmitAndNotTryAgainImmediately() {
        when(statusLine.getStatusCode()).thenReturn(500);
        setupOrderedDeliveryQueueWith(List.of("first-external-id"));

        database.inTransaction(() -> {
            webhookMessagePollingService.pollWebhookMessageQueue();
            try {
            verify(webhookMessageSenderMock).sendWebhookMessage(argThat((webhookMessage -> webhookMessage.getExternalId().equals("first-external-id"))));
            } catch (IOException | InvalidKeyException | InterruptedException ignored) { fail(); }
            webhookMessagePollingService.pollWebhookMessageQueue();
            verifyNoMoreInteractions(webhookMessageSenderMock);
        });
    }

    private void setupOrderedDeliveryQueueWith(List<String> messageIds) {
        WebhookEntity webhookEntity = new WebhookEntity();

        database.inTransaction(() -> {
            webhookEntity.setCallbackUrl("https://a-callback-url.test");
            webhookEntity.setServiceId("a-service-id");
            webhookEntity.setLive(false);

            webhookDao.create(webhookEntity);

            var eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_STARTED);
            database.getSessionFactory().getCurrentSession().persist(eventTypeEntity);
        });

        IntStream.range(0, messageIds.size()).forEach(i -> {
            var message = database.inTransaction(() -> {
                WebhookMessageEntity webhookMessageEntity = new WebhookMessageEntity();
                var id = messageIds.get(i);
                webhookMessageEntity.setExternalId(id);
                webhookMessageEntity.setWebhookEntity(webhookEntity);
                webhookMessageEntity.setEventType(eventTypeDao.findByName(EventTypeName.CARD_PAYMENT_STARTED).get());
                webhookMessageEntity.setCreatedDate(instantSource.instant());

                return webhookMessageDao.create(webhookMessageEntity);
            });
            database.inTransaction(() -> {
                webhookDeliveryQueueDao.enqueueFrom(message, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant().minusMillis((messageIds.size() - i) * 10L));
            });
        });
    }
}

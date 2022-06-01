package uk.gov.pay.webhooks.deliveryqueue.dao;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


@ExtendWith(DropwizardExtensionsSupport.class)
@ExtendWith(MockitoExtension.class)

class WebhookDeliveryQueueDaoTest {

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(EventTypeEntity.class)
            .addEntityClass(WebhookMessageEntity.class)
            .addEntityClass(WebhookEntity.class)
            .addEntityClass(WebhookDeliveryQueueEntity.class)
            .build();

    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private InstantSource instantSource;
    private WebhookMessageDao webhookMessageDao;
    
    @Mock
    MetricRegistry mockMetricRegistry;

    @BeforeEach
    void setUp() {
        instantSource = InstantSource.fixed(Instant.now());
        webhookMessageDao = new WebhookMessageDao(database.getSessionFactory());
        webhookDeliveryQueueDao = new WebhookDeliveryQueueDao(database.getSessionFactory(), instantSource);
    }

    @Test
    void nextToSendReturnsEnqueuedMessage() {
        WebhookMessageEntity persisted = database.inTransaction(() -> {
            WebhookMessageEntity webhookMessageEntity = new WebhookMessageEntity();
            webhookMessageEntity.setCreatedDate(instantSource.instant());
            return webhookMessageDao.create(webhookMessageEntity);
        });
        database.inTransaction(() -> {
            webhookDeliveryQueueDao.enqueueFrom(persisted, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant().minusMillis(1));
            assertThat(webhookDeliveryQueueDao.nextToSend(instantSource.instant()).get().getWebhookMessageEntity(), is(persisted));
        });
    }

    @Test
    void recordResultUpdatesAttempt() {
        WebhookMessageEntity persisted = database.inTransaction(() -> {
            WebhookMessageEntity webhookMessageEntity = new WebhookMessageEntity();
            webhookMessageEntity.setCreatedDate(instantSource.instant());
            return webhookMessageDao.create(webhookMessageEntity);
        });
        database.inTransaction(() -> {
            var enqueued = webhookDeliveryQueueDao.enqueueFrom(persisted, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant().minusMillis(1));
            var updated = webhookDeliveryQueueDao.recordResult(enqueued, "200 OK", Duration.ofMillis(50L), 200, WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL, mockMetricRegistry);
            assertThat(updated.getDeliveryStatus(), is(WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL));
            assertThat(updated.getDeliveryResult(), is("200 OK"));
            assertThat(updated.getStatusCode(), is(200));
        });
    }

    @Test
    void countFailed() {
        WebhookMessageEntity persisted = database.inTransaction(() -> {
            WebhookMessageEntity webhookMessageEntity = new WebhookMessageEntity();
            webhookMessageEntity.setCreatedDate(instantSource.instant());
            return webhookMessageDao.create(webhookMessageEntity);
        });
        database.inTransaction(() -> {
            webhookDeliveryQueueDao.enqueueFrom(persisted, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, instantSource.instant());
            webhookDeliveryQueueDao.enqueueFrom(persisted, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, instantSource.instant());
            assertThat(webhookDeliveryQueueDao.countFailed(persisted), is(2L));
        });
    }
}


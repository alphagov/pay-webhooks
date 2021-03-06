package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import java.time.Instant;
import java.time.InstantSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ExtendWith(DropwizardExtensionsSupport.class)
class WebhookMessageDaoTest {
    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(WebhookEntity.class)
            .addEntityClass(WebhookMessageEntity.class)
            .addEntityClass(WebhookDeliveryQueueEntity.class)
            .addEntityClass(EventTypeEntity.class)
            .build();

    private WebhookDao webhookDao;
    private WebhookMessageDao webhookMessageDao;
    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    String webhookExternalId = "webhook-external-id";

    @BeforeEach
    public void setUp() {
        webhookDao = new WebhookDao(database.getSessionFactory());
        webhookMessageDao = new WebhookMessageDao(database.getSessionFactory());
        webhookDeliveryQueueDao = new WebhookDeliveryQueueDao(database.getSessionFactory(), InstantSource.fixed(Instant.now()));
    }

   @Test
   public void shouldListAndCountAllWithNoStatus() {
        setup(1);
        var messages = webhookMessageDao.list(webhookExternalId, null, 1);
        var total = webhookMessageDao.count(webhookExternalId, null);
        assertThat(messages.size(), is(2));
        assertThat(total, is(2L));
   }

    @Test
    public void shouldListAndCountFilteredByStatus() {
        setup(1);
        var messages = webhookMessageDao.list(webhookExternalId, "SUCCESSFUL", 1);
        var total = webhookMessageDao.count(webhookExternalId, "SUCCESSFUL");
        assertThat(messages.size(), is(1));
        assertThat(total, is(1L));
        assertThat(messages.get(0).getExternalId(), is("successful-message-external-id"));
    }

    @Test
    public void shouldCalculateCorrectPagePosition() {
        setup(15);
        var firstPage = webhookMessageDao.list(webhookExternalId, null, 1);
        var secondPage = webhookMessageDao.list(webhookExternalId, null, 2);
        var total = webhookMessageDao.count(webhookExternalId, null);
        assertThat(firstPage.size(), is(10));
        assertThat(secondPage.size(), is(6));
        assertThat(total, is(16L));
    }

   private void setup(int numberOfPendingMessagesToPad) {
       var webhook = new WebhookEntity();
       webhook.setLive(true);
       webhook.setServiceId("real-service-id");
       webhook.setExternalId(webhookExternalId);

       var message = new WebhookMessageEntity();
       message.setWebhookEntity(webhook);
       message.setExternalId("successful-message-external-id");

       database.inTransaction(() -> {
           webhookDao.create(webhook);
           webhookMessageDao.create(message);
           webhookDeliveryQueueDao.enqueueFrom(message, WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL, Instant.now());

           for (var i = 0; i < numberOfPendingMessagesToPad; i++) {
               var padMessage = new WebhookMessageEntity();
               padMessage.setWebhookEntity(webhook);
               padMessage.setExternalId("padded-message-%s".formatted(i));
               webhookMessageDao.create(padMessage);
               webhookDeliveryQueueDao.enqueueFrom(padMessage, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, Instant.now());
           }
       });
   }
}

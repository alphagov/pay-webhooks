package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
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
    public void shouldDeleteWebhookMessages() {
        database.inTransaction(() -> {
            webhookMessageDao.deleteMessages(7, 15000);
        });
    }
    
   @Test
   public void shouldSerialiseAndDeserialiseWebhookMessage() {
        setup(0);
        var webhook = webhookDao.findByExternalId(webhookExternalId).get();
        var message = webhookMessageDao.get(webhook, "successful-message-external-id").get();
        assertThat(message.getExternalId(), is("successful-message-external-id"));
        assertThat(message.getWebhookEntity().getExternalId(), is(webhookExternalId));
        assertThat(message.getLastDeliveryStatus(), is(DeliveryStatus.SUCCESSFUL));
   }

   @Test
   public void shouldListAndCountAllWithNoStatus() {
        setup(1);
        var webhook = webhookDao.findByExternalId(webhookExternalId).get();
        var messages = webhookMessageDao.list(webhook, null, 1);
        var total = webhookMessageDao.count(webhook, null);
        assertThat(messages.size(), is(2));
        assertThat(total, is(2L));
   }

    @Test
    public void shouldListAndCountFilteredByStatus() {
        setup(1);
        var webhook = webhookDao.findByExternalId(webhookExternalId).get();
        var messages = webhookMessageDao.list(webhook, DeliveryStatus.SUCCESSFUL, 1);
        var total = webhookMessageDao.count(webhook, DeliveryStatus.SUCCESSFUL);
        assertThat(messages.size(), is(1));
        assertThat(total, is(1L));
        assertThat(messages.get(0).getExternalId(), is("successful-message-external-id"));
    }

    @Test
    public void shouldCalculateCorrectPagePosition() {
        setup(15);
        var webhook = webhookDao.findByExternalId(webhookExternalId).get();
        var firstPage = webhookMessageDao.list(webhook, null, 1);
        var secondPage = webhookMessageDao.list(webhook, null, 2);
        var total = webhookMessageDao.count(webhook, null);
        assertThat(firstPage.size(), is(10));
        assertThat(secondPage.size(), is(6));
        assertThat(total, is(16L));
    }

    private WebhookMessageEntity createWebhookMessageEntity(WebhookEntity webhook) {
        var message = new WebhookMessageEntity();
        message.setWebhookEntity(webhook);
        message.setLastDeliveryStatus(DeliveryStatus.SUCCESSFUL);
        message.setCreatedDate(Instant.now());
        return message;
    }

   private void setup(int numberOfPendingMessagesToPad) {
       var webhook = new WebhookEntity();
       webhook.setLive(true);
       webhook.setServiceId("real-service-id");
       webhook.setExternalId(webhookExternalId);

       var message = new WebhookMessageEntity();
       message.setWebhookEntity(webhook);
       message.setLastDeliveryStatus(DeliveryStatus.SUCCESSFUL);
       message.setExternalId("successful-message-external-id");

       database.inTransaction(() -> {
           webhookDao.create(webhook);
           webhookMessageDao.create(message);
           webhookDeliveryQueueDao.enqueueFrom(message, DeliveryStatus.SUCCESSFUL, Instant.now());

           for (var i = 0; i < numberOfPendingMessagesToPad; i++) {
               var padMessage = new WebhookMessageEntity();
               padMessage.setWebhookEntity(webhook);
               padMessage.setExternalId("padded-message-%s".formatted(i));
               webhookMessageDao.create(padMessage);
               webhookDeliveryQueueDao.enqueueFrom(padMessage, DeliveryStatus.PENDING, Instant.now());
           }
       });
   }
}

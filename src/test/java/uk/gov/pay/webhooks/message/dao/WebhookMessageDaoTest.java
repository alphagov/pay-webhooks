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
import uk.gov.pay.webhooks.webhook.resource.WebhookMessageSearchParams;

import java.time.Instant;
import java.time.InstantSource;
import java.util.UUID;

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
    public void shouldListAll() {
        setup(1);
        var webhook = webhookDao.findByExternalId(webhookExternalId).get();

        var searchParams = new WebhookMessageSearchParams(1, null, null);
        var messages = webhookMessageDao.list(webhook, searchParams);
        assertThat(messages.size(), is(2));
    }

    @Test
    public void shouldCountMessages() {
        setup(1);
        var webhook = webhookDao.findByExternalId(webhookExternalId).get();

        var searchParams = new WebhookMessageSearchParams(1, null, null);
        var count = webhookMessageDao.getTotalMessagesCount(webhook, searchParams);
        assertThat(count, is(2L));
    }
    
    @Test
    public void shouldCountMessagesByStatus() {
        setup(1);
        var webhook = webhookDao.findByExternalId(webhookExternalId).get();

        var searchParams = new WebhookMessageSearchParams(1, DeliveryStatus.SUCCESSFUL, null);
        var count = webhookMessageDao.getTotalMessagesCount(webhook, searchParams);
        assertThat(count, is(1L));
    }

    @Test
    public void shouldListFilteredMessages() {
        WebhookEntity webhook = insertWebhook();
        String resourceExternalId = "resource-external-id";
        WebhookMessageEntity matchedMessage = insertMessage(webhook, resourceExternalId, DeliveryStatus.SUCCESSFUL);
        insertMessage(webhook, resourceExternalId, DeliveryStatus.FAILED);
        insertMessage(webhook, "another-resource-external-id", DeliveryStatus.SUCCESSFUL);

        var searchParams = new WebhookMessageSearchParams(1, DeliveryStatus.SUCCESSFUL, resourceExternalId);
        var messages = webhookMessageDao.list(webhook, searchParams);
        assertThat(messages.size(), is(1));
        assertThat(messages.get(0).getExternalId(), is(matchedMessage.getExternalId()));
    }

    @Test
    public void shouldCalculateCorrectPagePosition() {
        setup(15);
        var webhook = webhookDao.findByExternalId(webhookExternalId).get();

        var firstPageParams = new WebhookMessageSearchParams(1, null, null);
        var firstPage = webhookMessageDao.list(webhook, firstPageParams);

        var secondPageParams = new WebhookMessageSearchParams(2, null, null);
        var secondPage = webhookMessageDao.list(webhook, secondPageParams);

        assertThat(firstPage.size(), is(10));
        assertThat(secondPage.size(), is(6));
    }

    private WebhookMessageEntity createWebhookMessageEntity(WebhookEntity webhook) {
        var message = new WebhookMessageEntity();
        message.setWebhookEntity(webhook);
        message.setLastDeliveryStatus(DeliveryStatus.SUCCESSFUL);
        message.setCreatedDate(Instant.now());
        return message;
    }

    private WebhookEntity insertWebhook() {
        var webhook = new WebhookEntity();
        webhook.setLive(true);
        webhook.setServiceId("real-service-id");
        webhook.setExternalId(webhookExternalId);

        database.inTransaction(() -> {
            webhookDao.create(webhook);
        });
        
        return webhook;
    }
    
    private WebhookMessageEntity insertMessage(WebhookEntity webhook, String resourceExternalId, DeliveryStatus deliveryStatus) {
        var message = new WebhookMessageEntity();
        message.setWebhookEntity(webhook);
        message.setLastDeliveryStatus(deliveryStatus);
        message.setExternalId(UUID.randomUUID().toString());
        message.setResourceExternalId(resourceExternalId);

        database.inTransaction(() -> {
            webhookMessageDao.create(message);
            webhookDeliveryQueueDao.enqueueFrom(message, DeliveryStatus.SUCCESSFUL, Instant.now());
        });
        
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

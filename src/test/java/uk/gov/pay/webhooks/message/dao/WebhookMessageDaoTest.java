package uk.gov.pay.webhooks.message.dao;

import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import java.time.Instant;
import java.time.InstantSource;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;


@ExtendWith(DropwizardExtensionsSupport.class)
class WebhookMessageDaoTest {

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(EventTypeEntity.class)
            .addEntityClass(WebhookMessageEntity.class)
            .addEntityClass(WebhookEntity.class)
            .addEntityClass(WebhookDeliveryQueueEntity.class)
            .build();


    private WebhookDao webhookDao;
    private WebhookMessageDao webhookMessageDao;
    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private InstantSource instantSource;

    @BeforeEach
    public void setUp() {
        instantSource = InstantSource.fixed(Instant.now());
        webhookDao = new WebhookDao(database.getSessionFactory());
        webhookMessageDao = new WebhookMessageDao(database.getSessionFactory());
        webhookDeliveryQueueDao = new WebhookDeliveryQueueDao(database.getSessionFactory(), instantSource);
    }

    @Test
    public void webhookMessageIncludesAttempts() {
        WebhookEntity webhook = database.inTransaction(() -> {
            WebhookEntity webhookEntity = new WebhookEntity();
            EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
            webhookEntity.addSubscription(eventTypeEntity);
            webhookEntity.setCreatedDate(Date.from(instantSource.instant()));
            return webhookDao.create(webhookEntity);
        });

        WebhookMessageEntity webhookMessage = database.inTransaction(() -> {
            WebhookMessageEntity webhookMessageEntity = new WebhookMessageEntity();
            webhookMessageEntity.setWebhookEntity(webhook);
            webhookMessageEntity.setExternalId("some-external-id");
            return webhookMessageDao.create(webhookMessageEntity);
        });

        WebhookDeliveryQueueEntity queueAttempt = database.inTransaction(() -> {
            return webhookDeliveryQueueDao.enqueueFrom(webhookMessage, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, Date.from(instantSource.instant()));
        });

        assertThat(webhookMessage.getDeliveryAttempts(), is(List.of(queueAttempt)));
    }
}





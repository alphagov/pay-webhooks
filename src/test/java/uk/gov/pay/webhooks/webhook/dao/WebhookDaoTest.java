package uk.gov.pay.webhooks.webhook.dao;

import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.iterableWithSize;

@ExtendWith(DropwizardExtensionsSupport.class)
public class WebhookDaoTest {

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(WebhookEntity.class)
            .addEntityClass(EventTypeEntity.class)
            .build();

    private WebhookDao webhookDao;

    @BeforeEach
    public void setUp() {
        webhookDao = new WebhookDao(database.getSessionFactory());
    }

    @Test
    public void persistsSubscriptions() {
        
        WebhookEntity persisted = database.inTransaction(() -> {
            WebhookEntity webhookEntity = new WebhookEntity();
            EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
            webhookEntity.addSubscription(eventTypeEntity);
            return webhookDao.create(webhookEntity);
        });

        assertThat(persisted.getSubscriptions(), iterableWithSize(1));
        assertThat(persisted.getSubscriptions(), containsInAnyOrder(any(EventTypeEntity.class)));
        assertThat(persisted.getSubscriptions().iterator().next().getName(), is(EventTypeName.CARD_PAYMENT_CAPTURED));
    }    
    
    @Test
    public void returnsEmptyListIfNoWebhooksFound() {
        assertThat(webhookDao.list(true, "not-real-service-id"), iterableWithSize(0));
    }
    
    @Test
    public void returnsMatchingListOfWebhooks() {
        database.inTransaction(() -> {
            WebhookEntity webhookEntity = new WebhookEntity();
            webhookEntity.setLive(true);
            webhookEntity.setServiceId("real-service-id");
            EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
            webhookEntity.addSubscription(eventTypeEntity);
            webhookDao.create(webhookEntity);
            assertThat(webhookDao.list(true, "real-service-id"), iterableWithSize(1));
        });
    }

    @Test
    public void filtersWebhooksByLiveStatus() {
        database.inTransaction(() -> {
            WebhookEntity webhookEntity = new WebhookEntity();
            webhookEntity.setLive(true);
            webhookEntity.setServiceId("not-real-service-id");
            EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
            webhookEntity.addSubscription(eventTypeEntity);
            webhookDao.create(webhookEntity);
            assertThat(webhookDao.list(false, "not-real-service-id"), iterableWithSize(0));
        });
    }
        
    @Test
    public void filtersWebhooksByServiceId() {
        database.inTransaction(() -> {
            WebhookEntity webhookEntity = new WebhookEntity();
            webhookEntity.setLive(true);
            webhookEntity.setServiceId("not-real-service-id");
            EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
            webhookEntity.addSubscription(eventTypeEntity);
            webhookDao.create(webhookEntity);
            assertThat(webhookDao.list(true, "real-service-id"), iterableWithSize(0));
        });
    }    
    
}

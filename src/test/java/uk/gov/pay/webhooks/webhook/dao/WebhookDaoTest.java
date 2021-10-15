package uk.gov.pay.webhooks.webhook.dao;

import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
        assertThat(webhookDao.list(true, "not-real-service-id", null), iterableWithSize(0));
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
        });
        
        assertThat(webhookDao.list(true, "real-service-id", null), iterableWithSize(1));
    }
    
    @ParameterizedTest
    @CsvSource({"false, false,1", "true, false, 0"})
    public void filtersWebhooksByLiveStatus(boolean setLive, boolean queryLive,  int numberOfExpectedResults) {
        database.inTransaction(() -> {
            WebhookEntity webhookEntity = new WebhookEntity();
            webhookEntity.setLive(setLive);
            
            webhookEntity.setServiceId("not-real-service-id");
            EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
            
            webhookEntity.addSubscription(eventTypeEntity);
            webhookDao.create(webhookEntity);
        });
            assertThat(webhookDao.list(queryLive, "not-real-service-id", null), iterableWithSize(numberOfExpectedResults));
    }

    @ParameterizedTest
    @CsvSource({"real-service-id,1", "not-real-service-id,0"})
    public void filtersWebhooksByServiceId(String serviceId, int numberOfExpectedResults) {
        database.inTransaction(() -> {
            WebhookEntity webhookEntity = new WebhookEntity();
            webhookEntity.setLive(true);
            
            webhookEntity.setServiceId(serviceId);
            EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
            
            webhookEntity.addSubscription(eventTypeEntity);
            webhookDao.create(webhookEntity);
        });
        
            assertThat(webhookDao.list(true, "real-service-id", null), iterableWithSize(numberOfExpectedResults));
    }

    @Test
    public void listsAllWebhooksWhenOverrideParamProvided() {
        database.inTransaction(() -> {
            WebhookEntity webhookEntityOne = new WebhookEntity();
            webhookEntityOne.setLive(true);
            webhookEntityOne.setServiceId("service1");
            webhookDao.create(webhookEntityOne);            
            
            WebhookEntity webhookEntityTwo = new WebhookEntity();
            webhookEntityTwo.setLive(true);
            webhookEntityTwo.setServiceId("service2");
            webhookDao.create(webhookEntityTwo);
        });

        assertThat(webhookDao.list(true, null, true), iterableWithSize(2));
    }

}

package uk.gov.pay.webhooks.webhook.dao;

import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import java.time.Instant;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
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
    void getsWebhookByExternalIDAndGatewayAccountID() {
        var webhookEntity = new WebhookEntity();
        webhookEntity.setExternalId("external-id");
        webhookEntity.setLive(true);
        webhookEntity.setServiceId("real-service-id");
        webhookEntity.setGatewayAccountId("100");
        database.inTransaction(() -> {
            webhookDao.create(webhookEntity);
        });

        assertThat(webhookDao.findByExternalIdAndGatewayAccountId("external-id", "100"), is(Optional.of(webhookEntity)));
        assertThat(webhookDao.findByExternalIdAndGatewayAccountId("external-id", "200"), is(Optional.empty()));
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
        
        assertThat(webhookDao.list(true, "real-service-id"), iterableWithSize(1));
    }
    
    @Test
    public void filtersWebhooksByLiveStatusFalse() {
        database.inTransaction(() -> {
            WebhookEntity webhookEntityLiveFalse = new WebhookEntity();
            webhookEntityLiveFalse.setLive(false);
            webhookEntityLiveFalse.setServiceId("service-id");
            webhookDao.create(webhookEntityLiveFalse);
            
            WebhookEntity webhookEntityLiveTrue = new WebhookEntity();
            webhookEntityLiveTrue.setLive(true);
            webhookEntityLiveTrue.setServiceId("service-id");
            webhookDao.create(webhookEntityLiveTrue);
        });
        
            assertThat(webhookDao.list(false, "service-id"), iterableWithSize(1));
    }    
    
    @Test
    public void filtersWebhooksByLiveStatusTrue() {
        database.inTransaction(() -> {
            WebhookEntity webhookEntityLiveTrue = new WebhookEntity();
            webhookEntityLiveTrue.setLive(true);
            webhookEntityLiveTrue.setServiceId("service-id");
            webhookDao.create(webhookEntityLiveTrue);
            
            WebhookEntity webhookEntityLiveFalse = new WebhookEntity();
            webhookEntityLiveFalse.setLive(false);
            webhookEntityLiveFalse.setServiceId("service-id");
            webhookDao.create(webhookEntityLiveFalse);

        });
            assertThat(webhookDao.list(true, "service-id"), iterableWithSize(1));
    }
    
    @Test
    public void notFoundEntityReturnsEmptyOption(){
        assertThat(webhookDao.findByExternalIdAndServiceId("foo", "bar").isEmpty(), equalTo(true));
    }     

    @Test
    public void filtersWebhooksByServiceId() {
        database.inTransaction(() -> {
            WebhookEntity webhookEntity = new WebhookEntity();
            webhookEntity.setLive(true);
            
            webhookEntity.setServiceId("service-id-1");
            EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
            
            webhookEntity.addSubscription(eventTypeEntity);
            webhookDao.create(webhookEntity);
        });
        
            assertThat(webhookDao.list(true, "service-id-1"), iterableWithSize(1));
            assertThat(webhookDao.list(true, "service-id-2"), iterableWithSize(0));
    }    
    
    @Test
    public void listsAllWebhooks() {
        database.inTransaction(() -> {
            WebhookEntity webhookEntityServiceOne = new WebhookEntity();
            webhookEntityServiceOne.setLive(true);
            webhookEntityServiceOne.setServiceId("service-id-1");
            webhookDao.create(webhookEntityServiceOne);

            WebhookEntity webhookEntityServiceTwo = new WebhookEntity();
            webhookEntityServiceTwo.setLive(true);
            webhookEntityServiceTwo.setServiceId("service-id-2");
            webhookDao.create(webhookEntityServiceTwo);
        }); 
            
        assertThat(webhookDao.list(true), iterableWithSize(2));
    }    
    
    @Test
    public void listsAllWebhooksOrderedByCreatedDate() {
        database.inTransaction(() -> {
            WebhookEntity webhookEntityServiceOne = new WebhookEntity();
            webhookEntityServiceOne.setLive(true);
            webhookEntityServiceOne.setServiceId("service-id-1");
            webhookEntityServiceOne.setCreatedDate(Instant.parse("2007-12-03T10:15:30.00Z"));
            webhookDao.create(webhookEntityServiceOne);

            WebhookEntity webhookEntityServiceTwo = new WebhookEntity();
            webhookEntityServiceTwo.setLive(true);
            webhookEntityServiceTwo.setServiceId("service-id-newer-created-date");
            webhookEntityServiceTwo.setCreatedDate(Instant.parse("2020-12-03T10:15:30.00Z"));
            webhookDao.create(webhookEntityServiceTwo);
        });

        assertThat(webhookDao.list(true).stream().map(WebhookEntity::getCreatedDate).toList(),
                contains((Instant.parse("2020-12-03T10:15:30.00Z")),(Instant.parse("2007-12-03T10:15:30.00Z"))));
    }

    @Test
    public void listsAllWebhooksFilteredByLive() {
        database.inTransaction(() -> {
            WebhookEntity webhookEntityLiveTrue = new WebhookEntity();
            webhookEntityLiveTrue.setLive(true);
            webhookEntityLiveTrue.setServiceId("service-id");
            webhookDao.create(webhookEntityLiveTrue);

            WebhookEntity webhookEntityLiveFalse = new WebhookEntity();
            webhookEntityLiveFalse.setLive(false);
            webhookEntityLiveFalse.setServiceId("service-id");
            webhookDao.create(webhookEntityLiveFalse);            
            
            WebhookEntity webhookEntityLiveFalseTwo = new WebhookEntity();
            webhookEntityLiveFalseTwo.setLive(false);
            webhookEntityLiveFalseTwo.setServiceId("service-id-2");
            webhookDao.create(webhookEntityLiveFalseTwo);
        });
        var response = webhookDao.list(false);
        assertThat(response, iterableWithSize(2));
        response.forEach(webhookEntity -> 
                assertThat(webhookEntity.isLive(), equalTo(false)));
    }

    @Test
    void listWebhooksByGatewayAccountId() {
        database.inTransaction(() -> {
            var serviceOneFirst = new WebhookEntity();
            serviceOneFirst.setLive(true);
            serviceOneFirst.setServiceId("service-id-1");
            serviceOneFirst.setGatewayAccountId("100");
            webhookDao.create(serviceOneFirst);

            var serviceOneSecond = new WebhookEntity();
            serviceOneSecond.setLive(true);
            serviceOneSecond.setServiceId("service-id-1");
            serviceOneSecond.setGatewayAccountId("100");
            webhookDao.create(serviceOneSecond);

            WebhookEntity serviceTwoFirst = new WebhookEntity();
            serviceTwoFirst.setLive(true);
            serviceTwoFirst.setServiceId("service-id-2");
            serviceTwoFirst.setGatewayAccountId("200");
            webhookDao.create(serviceTwoFirst);
        });
        var response = webhookDao.listByGatewayAccountId("100");
        assertThat(response, iterableWithSize(2));
        response.forEach(webhookEntity ->
                assertThat(webhookEntity.getGatewayAccountId(), equalTo("100")));
        
    }
}

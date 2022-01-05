package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.WebhookMessageSignatureGenerator;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Date;

import static org.mockito.Mockito.when;

@ExtendWith(DropwizardExtensionsSupport.class)
class WebhookMessageSendingQueueProcessorTest {

    public DAOTestExtension database = DAOTestExtension.newBuilder()
            .addEntityClass(EventTypeEntity.class)
            .addEntityClass(WebhookMessageEntity.class)
            .addEntityClass(WebhookEntity.class)
            .addEntityClass(WebhookDeliveryQueueEntity.class)
            .build();

    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private InstantSource instantSource;
    private WebhookMessageDao webhookMessageDao;
    private WebhookDao webhookDao;

    @Mock
    private Environment environment;
    
    @Mock
    private HttpClient httpClient;
    
    @Mock
    private HttpResponse httpResponse;
            
    @BeforeEach
    void setUp(){
        instantSource = InstantSource.fixed(Instant.now());
        webhookMessageDao = new WebhookMessageDao(database.getSessionFactory());
        webhookDao = new WebhookDao(database.getSessionFactory());
        webhookDeliveryQueueDao = new WebhookDeliveryQueueDao(database.getSessionFactory(), instantSource);
    }
    
    @Test
    void processQueue() throws IOException, InterruptedException {
        
        HttpResponse<String[]> mockedResponse = Mockito.mock(HttpResponse.class);
        when(mockedResponse.statusCode()).thenReturn(404);
        var sendAttempter = new SendAttempter(webhookDeliveryQueueDao, httpClient, new ObjectMapper(), new WebhookMessageSignatureGenerator(), instantSource);

        WebhookMessageEntity persisted = database.inTransaction(() -> {
            WebhookEntity webhookEntityServiceOne = new WebhookEntity();
            webhookEntityServiceOne.setLive(true);
            webhookEntityServiceOne.setServiceId("service-id-1");
            webhookEntityServiceOne.setCreatedDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z")));
            webhookEntityServiceOne.setCallbackUrl("http://example.com");
            webhookEntityServiceOne.setSigningKey("some-signing-key");
            webhookDao.create(webhookEntityServiceOne);
            WebhookMessageEntity webhookMessageEntity = new WebhookMessageEntity();
            webhookMessageEntity.setWebhookEntity(webhookEntityServiceOne);
            webhookMessageEntity.setCreatedDate(Date.from(instantSource.instant()));
            return webhookMessageDao.create(webhookMessageEntity);
        });
        database.inTransaction(() -> {
            var enqueuedItem = webhookDeliveryQueueDao.enqueueFrom(persisted, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, Date.from(instantSource.instant().minusMillis(1)));
            sendAttempter.attemptSend(enqueuedItem);
        });
        
        
    }
    
    
}

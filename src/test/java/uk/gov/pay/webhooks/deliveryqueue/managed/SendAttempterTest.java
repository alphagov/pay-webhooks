package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.junit5.DAOTestExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.WebhookMessageSender;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.validations.CallbackUrlDomainNotOnAllowListException;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.security.InvalidKeyException;
import java.time.Instant;
import java.time.InstantSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ExtendWith(DropwizardExtensionsSupport.class)
class SendAttempterTest {

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
    private WebhookMessageEntity webhookMessageEntity;
    
    @Mock
    private WebhookMessageSender mockWebhookMessageSender;
    
    @Mock
    private HttpResponse mockHttpResponse;
    
    @Mock
    private Environment mockEnvironment;    
    
    @Mock
    private MetricRegistry mockMetricRegistry;
            
    @BeforeEach
    void setUp(){
        instantSource = InstantSource.fixed(Instant.now());
        webhookMessageDao = new WebhookMessageDao(database.getSessionFactory());
        webhookDao = new WebhookDao(database.getSessionFactory());
        webhookDeliveryQueueDao = new WebhookDeliveryQueueDao(database.getSessionFactory(), instantSource);
        webhookMessageEntity = new WebhookMessageEntity();
        WebhookEntity webhookEntity = new WebhookEntity();
        webhookEntity.setLive(true);
        webhookEntity.setServiceId("service-id-1");
        webhookEntity.setCreatedDate(Instant.parse("2007-12-03T10:15:30.00Z"));
        webhookEntity.setCallbackUrl("http://example.com");
        webhookEntity.setSigningKey("some-signing-key");
        webhookDao.create(webhookEntity);
        webhookMessageEntity.setWebhookEntity(webhookEntity);
        webhookMessageEntity.setCreatedDate(instantSource.instant());
        given(mockEnvironment.metrics()).willReturn(mockMetricRegistry);
    }
    
    @Test
    void sendAttempterSetsDeliveryStatusBasedOnStatusCode() throws IOException, InterruptedException, InvalidKeyException {
        given(mockWebhookMessageSender.sendWebhookMessage(any(WebhookMessageEntity.class))).willReturn(mockHttpResponse);
        
        var webhookMessage = webhookMessageDao.create(webhookMessageEntity);
        var sendAttempter = new SendAttempter(webhookDeliveryQueueDao, instantSource, mockWebhookMessageSender, mockEnvironment);
        var enqueuedItem = webhookDeliveryQueueDao.enqueueFrom(webhookMessage, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant());
        given(mockHttpResponse.statusCode()).willReturn(404, 200);
        
        sendAttempter.attemptSend(enqueuedItem);
        assertThat(enqueuedItem.getDeliveryStatus(), is(WebhookDeliveryQueueEntity.DeliveryStatus.FAILED));
        assertThat(enqueuedItem.getDeliveryResult(), is("404 Not Found"));
        sendAttempter.attemptSend(enqueuedItem);
        assertThat(enqueuedItem.getDeliveryStatus(), is(WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL));
        assertThat(enqueuedItem.getDeliveryResult(), is("200 OK"));
    }    
    
    @Test
    void sendAttempterEmitsDeliveryStatusMetric() throws IOException, InvalidKeyException, InterruptedException {
        given(mockWebhookMessageSender.sendWebhookMessage(any(WebhookMessageEntity.class))).willReturn(mockHttpResponse);
        
        var webhookMessage = webhookMessageDao.create(webhookMessageEntity);
        var sendAttempter = new SendAttempter(webhookDeliveryQueueDao, instantSource, mockWebhookMessageSender, mockEnvironment);
        var enqueuedItem = webhookDeliveryQueueDao.enqueueFrom(webhookMessage, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant());
        given(mockHttpResponse.statusCode()).willReturn(200);
        sendAttempter.attemptSend(enqueuedItem);
        verify(mockMetricRegistry).counter("delivery-status.SUCCESSFUL");
    }

    @Test
    void sendAttempterCatchesExceptions() throws IOException, InterruptedException, InvalidKeyException {

        var webhookMessage = webhookMessageDao.create(webhookMessageEntity);
        var sendAttempter = new SendAttempter(webhookDeliveryQueueDao, instantSource, mockWebhookMessageSender, mockEnvironment);
        var enqueuedItem = webhookDeliveryQueueDao.enqueueFrom(webhookMessage, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant());
        given(mockWebhookMessageSender.sendWebhookMessage(any(WebhookMessageEntity.class))).willThrow(IOException.class, HttpTimeoutException.class);

        assertDoesNotThrow(() -> sendAttempter.attemptSend(enqueuedItem));
        assertThat(enqueuedItem.getDeliveryStatus(), is(WebhookDeliveryQueueEntity.DeliveryStatus.FAILED));

        assertDoesNotThrow(() -> sendAttempter.attemptSend(enqueuedItem));
        assertThat(enqueuedItem.getDeliveryStatus(), is(WebhookDeliveryQueueEntity.DeliveryStatus.FAILED));
    }
    
    @Test
    void sendAttempterEnqueuesRetriesIfFailure() throws IOException, InvalidKeyException, InterruptedException {
        given(mockWebhookMessageSender.sendWebhookMessage(any(WebhookMessageEntity.class))).willReturn(mockHttpResponse);
        var webhookMessage = webhookMessageDao.create(webhookMessageEntity);
        var sendAttempter = new SendAttempter(webhookDeliveryQueueDao, instantSource, mockWebhookMessageSender, mockEnvironment);
        var enqueuedItem = webhookDeliveryQueueDao.enqueueFrom(webhookMessage, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant());
        given(mockHttpResponse.statusCode()).willReturn(404);
       
        sendAttempter.attemptSend(enqueuedItem);
        assertThat(enqueuedItem.getDeliveryStatus(), is(WebhookDeliveryQueueEntity.DeliveryStatus.FAILED));

        database.inTransaction(() -> {
            assertThat(webhookDeliveryQueueDao.nextToSend((Instant.parse("2007-12-03T10:15:30.00Z"))), is(notNullValue()));
            assertThat(webhookDeliveryQueueDao.countFailed(webhookMessageEntity), is(1L));
        });
    }

    @Test
    void sendAttempterDoesNotEnqueueRetryForRejectedForSecurityRules() throws IOException, InvalidKeyException, InterruptedException {
        given(mockWebhookMessageSender.sendWebhookMessage(any(WebhookMessageEntity.class))).willThrow(CallbackUrlDomainNotOnAllowListException.class);
        var webhookMessage = webhookMessageDao.create(webhookMessageEntity);
        var sendAttempter = new SendAttempter(webhookDeliveryQueueDao, instantSource, mockWebhookMessageSender, mockEnvironment);
        var enqueuedItem = webhookDeliveryQueueDao.enqueueFrom(webhookMessage, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant());

        sendAttempter.attemptSend(enqueuedItem);
        assertThat(enqueuedItem.getDeliveryStatus(), is(WebhookDeliveryQueueEntity.DeliveryStatus.WILL_NOT_SEND));
    }
}

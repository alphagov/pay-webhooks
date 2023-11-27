package uk.gov.pay.webhooks.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.pay.webhooks.app.WebhookMessageDeletionConfig;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeDao;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.queue.InternalEvent;
import uk.gov.pay.webhooks.util.IdGenerator;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.resource.CreateWebhookRequest;

import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookServiceTest {
    private final WebhookDao webhookDao = mock(WebhookDao.class);
    private final WebhookMessageDao webhookMessageDao = mock(WebhookMessageDao.class);
    private final WebhookDeliveryQueueDao webhookDeliveryQueueDao = mock(WebhookDeliveryQueueDao.class);
    private final EventTypeDao eventTypeDao = mock(EventTypeDao.class);
    private final InstantSource instantSource = InstantSource.fixed(Instant.now());
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private final WebhookMessageDeletionConfig webhookMessageDeletionConfig = mock(WebhookMessageDeletionConfig.class);
    private WebhookService webhookService;
    private final String serviceId = "test_service_id";
    private final String gatewayAccountId = "100";
    private final String callbackUrl = "test_callback_url";
    private final boolean live = true;
    private final String description = "test_description";
    
    @BeforeEach
    public void setUp() {
        webhookService = new WebhookService(webhookDao, eventTypeDao, instantSource, idGenerator, webhookMessageDao, 
                webhookDeliveryQueueDao, webhookMessageDeletionConfig);
    }
    
    @Test
    public void shouldDeleteWebhookMessages() {
        when(webhookMessageDeletionConfig.getMaxAgeOfMessages()).thenReturn(7);
        when(webhookMessageDeletionConfig.getMaxNumOfMessagesToExpire()).thenReturn(4);
        
        webhookService.deleteWebhookMessages();

        verify(webhookMessageDao).deleteMessages(7, 4);
    }
    
    private WebhookMessageEntity createWebhookMessageEntity(WebhookEntity webhook) {
        var message = new WebhookMessageEntity();
        message.setWebhookEntity(webhook);
        message.setLastDeliveryStatus(DeliveryStatus.SUCCESSFUL);
        message.setCreatedDate(Instant.now());
        return message;
    }

    @Test
    public void shouldCallWebhookDaoWithAllSetAttributes() {
        EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
        when(eventTypeDao.findByName(eq(EventTypeName.CARD_PAYMENT_CAPTURED)))
                .thenReturn(Optional.of(eventTypeEntity));
        
        var createWebhookRequest = new CreateWebhookRequest(
                serviceId,
                gatewayAccountId,
                live,
                callbackUrl,
                description,
                List.of(EventTypeName.CARD_PAYMENT_CAPTURED)
        );

        webhookService.createWebhook(createWebhookRequest);

        ArgumentCaptor<WebhookEntity> argumentCaptor = ArgumentCaptor.forClass(WebhookEntity.class);
        verify(webhookDao).create(argumentCaptor.capture());
        WebhookEntity captured = argumentCaptor.getAllValues().get(0);

        assertThat(
                captured.getSubscriptions()
                        .iterator().next().getName().getName(),
                is("card_payment_captured")
        );
        assertThat(captured.getServiceId(), is(serviceId));
        assertThat(captured.getGatewayAccountId(), is(gatewayAccountId));
        assertThat(captured.getCallbackUrl(), is(callbackUrl));
        assertThat(captured.getDescription(), is(description));
        assertThat(captured.isLive(), is(live));
    }

    @Test
    public void shouldCallWebhookDaoWithOnlyRequiredAttributes() {
        var createWebhookRequest = new CreateWebhookRequest(
                serviceId,
                gatewayAccountId,
                live,
                callbackUrl,
                null,
                null
        );

        webhookService.createWebhook(createWebhookRequest);

        ArgumentCaptor<WebhookEntity> argumentCaptor = ArgumentCaptor.forClass(WebhookEntity.class);
        verify(webhookDao).create(argumentCaptor.capture());
        WebhookEntity captured = argumentCaptor.getAllValues().get(0);

        assertThat(captured.getServiceId(), is(serviceId));
        assertThat(captured.getCallbackUrl(), is(callbackUrl));
        assertThat(captured.isLive(), is(live));    
    }
    
    @Test
    public void shouldReturnSubscribedWebhooksForGivenEventType() {
        var capturedEventType = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
        var webhookSubscribedToCaptureEvent = new WebhookEntity();
        webhookSubscribedToCaptureEvent.addSubscription(capturedEventType);
        var webhookNotSubscribedToAnyEvents = new WebhookEntity();
        when(webhookDao.listByGatewayAccountId(gatewayAccountId))
                .thenReturn(List.of(webhookSubscribedToCaptureEvent, webhookNotSubscribedToAnyEvents));
        var event = new InternalEvent("CAPTURE_CONFIRMED", serviceId, gatewayAccountId, live, "resource_id", null, instantSource.instant(), "PAYMENT");
        
        var subscribedWebhooks = webhookService.getWebhooksSubscribedToEvent(event);
        
        assertThat(subscribedWebhooks.size(), is(1));
        var subscribedWebhook = subscribedWebhooks.get(0);
        assertThat(subscribedWebhook.getSubscriptions(), hasItem(capturedEventType));
    }

    @Test
    public void shouldReturnSigningKey() {
        when(idGenerator.newWebhookSigningKey(live))
                .thenReturn("some-signing-key");
        
        var createWebhookRequest = new CreateWebhookRequest(
                serviceId,
                gatewayAccountId,
                live,
                callbackUrl,
                null,
                null
        );

        webhookService.createWebhook(createWebhookRequest);

        ArgumentCaptor<WebhookEntity> argumentCaptor = ArgumentCaptor.forClass(WebhookEntity.class);
        verify(webhookDao).create(argumentCaptor.capture());
        WebhookEntity captured = argumentCaptor.getAllValues().get(0);
        assertThat(captured.getSigningKey(), equalTo("some-signing-key"));
    }
    
    @Test
    public void printIt(){
        List<String> externalIdList = List.of(
                "second-message-external-id",
                "third-message-external-id",
                "fourth-message-external-id",
                "fifth-message-external-id",
                "sixth-message-external-id",
                "seventh-message-external-id",
                "eighth-message-external-id",
                "ninth-message-external-id",
                "tenth-message-external-id",
                "eleventh-message-external-id",
                "twelfth-message-external-id"
        );
        addWebhookMessage(2, 12, externalIdList, "2022-01-01", 1, "2022-01-01", 1,"{}", null, null, DeliveryStatus.valueOf("PENDING"));

    }

    public void addWebhookMessage(int startIdIndex, int recordCount, List<String> externalIdList, String createdDate, int webhookId, String eventDate, int eventType, String resource, String resourceExternalId, String resourceType, DeliveryStatus status) {
        for (int i = startIdIndex; i <= recordCount; i++) {
            System.out.println(i);
            System.out.println(externalIdList.get(i-2));
        }
    }
    
}

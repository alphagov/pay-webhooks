package uk.gov.pay.webhooks.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeDao;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.queue.InternalEvent;
import uk.gov.pay.webhooks.util.IdGenerator;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.resource.CreateWebhookRequest;

import java.time.Instant;
import java.time.InstantSource;
import java.time.ZonedDateTime;
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
    private final EventTypeDao eventTypeDao = mock(EventTypeDao.class);
    private final InstantSource instantSource = InstantSource.fixed(Instant.now());
    private final IdGenerator idGenerator = mock(IdGenerator.class);
    private WebhookService webhookService;
    private final String serviceId = "test_service_id";
    private final String callbackUrl = "test_callback_url";
    private final boolean live = true;
    private final String description = "test_description";
    
    @BeforeEach
    public void setUp() {
        webhookService = new WebhookService(webhookDao, eventTypeDao, instantSource, idGenerator);
    }

    @Test
    public void shouldCallWebhookDaoWithAllSetAttributes() {
        EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
        when(eventTypeDao.findByName(eq(EventTypeName.CARD_PAYMENT_CAPTURED)))
                .thenReturn(Optional.of(eventTypeEntity));
        
        var createWebhookRequest = new CreateWebhookRequest(
                serviceId,
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
        assertThat(captured.getCallbackUrl(), is(callbackUrl));
        assertThat(captured.getDescription(), is(description));
        assertThat(captured.isLive(), is(live));
    }

    @Test
    public void shouldCallWebhookDaoWithOnlyRequiredAttributes() {
        var createWebhookRequest = new CreateWebhookRequest(
                serviceId,
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
    public void shouldReturnSubscribedWebhooksForGivenEventTypeServiceIdLivenessTuple() {
        var capturedEventType = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
        var webhookSubscribedToCaptureEvent = new WebhookEntity();
        webhookSubscribedToCaptureEvent.addSubscription(capturedEventType);
        var webhookNotSubscribedToAnyEvents = new WebhookEntity();
        when(webhookDao.list(live, serviceId))
                .thenReturn(List.of(webhookSubscribedToCaptureEvent, webhookNotSubscribedToAnyEvents));
        var event = new InternalEvent("CAPTURE_CONFIRMED", serviceId, live, "resource_id", ZonedDateTime.now());
        
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
    
}


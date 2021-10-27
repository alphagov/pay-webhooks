package uk.gov.pay.webhooks.webhook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeDao;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.resource.CreateWebhookRequest;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebhookServiceTest {
    private WebhookDao webhookDao = mock(WebhookDao.class);
    private EventTypeDao eventTypeDao = mock(EventTypeDao.class);
    private WebhookService webhookService;
    private String serviceId = "test_service_id";
    private String callbackUrl = "test_callback_url";
    private boolean live = true;
    private String description = "test_description";
    
    @BeforeEach
    public void setUp() {
        webhookService = new WebhookService(webhookDao, eventTypeDao);
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
}


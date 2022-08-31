package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeDao;
import uk.gov.pay.webhooks.ledger.LedgerService;
import uk.gov.pay.webhooks.message.dao.WebhookMessageDao;
import uk.gov.pay.webhooks.queue.InternalEvent;
import uk.gov.pay.webhooks.util.IdGenerator;
import uk.gov.pay.webhooks.webhook.WebhookService;

import java.io.IOException;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebhookMessageServiceTest {
    @Mock
    WebhookService webhookService;
    @Mock
    LedgerService ledgerService;
    @Mock
    EventTypeDao eventTypeDao;
    @Mock
    WebhookMessageDao webhookMessageDao;
    @Mock
    WebhookDeliveryQueueDao webhookDeliveryQueueDao;

    WebhookMessageService webhookMessageService;

    @BeforeEach
    public void setUp() {
        webhookMessageService = new WebhookMessageService(webhookService, ledgerService, eventTypeDao, InstantSource.fixed(Instant.now()), new IdGenerator(), new ObjectMapper(), webhookMessageDao, webhookDeliveryQueueDao); 
    }

    @Test
    public void shouldIgnoreEventsWithoutRequiredProperties() throws IOException, InterruptedException {
        var eventWithProperties = new InternalEvent("PAYMENT_CREATED", "service-id", true, "resource-external-id", null, Instant.now(), "payment");
        var eventMissingProperties = new InternalEvent("PAYMENT_CREATED", null, null, "resource-external-id", null, Instant.now(), "payment");
        when(webhookService.getWebhooksSubscribedToEvent(eventWithProperties)).thenReturn(List.of());

        webhookMessageService.handleInternalEvent(eventWithProperties);
        verify(webhookService).getWebhooksSubscribedToEvent(eventWithProperties);

        webhookMessageService.handleInternalEvent(eventMissingProperties);
        verify(webhookService, never()).getWebhooksSubscribedToEvent(eventMissingProperties);
    }
}

package uk.gov.pay.webhooks.webhookevent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.pay.webhooks.queue.Event;
import uk.gov.pay.webhooks.webhook.WebhookService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class WebhookEventServiceTest {
    WebhookService webhookService = mock(WebhookService.class);
    
    WebhookEventService webhookEventService;
    
    @BeforeEach
    public void setUp() {
        webhookEventService = new WebhookEventService(webhookService);
    }
    
    @Test
    public void shouldPublishCapureConfirmedEvent() {
    }

}

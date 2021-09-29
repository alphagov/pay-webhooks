package uk.gov.pay.webhooks.webhook;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.resource.WebhookResource;

import javax.ws.rs.client.Entity;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DropwizardExtensionsSupport.class)
public class WebhookResourceTest {
   WebhookDao webhookDao = mock(WebhookDao.class);
   String existingWebhookId = "existing_webhook_id";

    public final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new WebhookResource(webhookDao))
            .build();
    
    WebhookEntity webhook;

    @BeforeEach
    void setup() {
        webhook = new WebhookEntity();
        webhook.setCreatedDate(Date.from(Instant.now()));
    }

    @Test
    public void createWebhookWithValidParams() {
        when(webhookDao.create(any(WebhookEntity.class))).thenReturn(webhook);
        var json = """
                {
                    "service_id": "some-service-id",
                    "callback_url": "https://some-callback-url.com",
                    "live": true
                }
                """;
        
        var response = resources
                .target("/v1/webhook")
                .request()
                .post(Entity.json(json));

        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void createWebhookWithMissingParamsRejected() {
        when(webhookDao.create(any(WebhookEntity.class))).thenReturn(webhook);
        var response = resources
                .target("/v1/webhook")
                .request()
                .post(Entity.json(""));

        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void createWebhookWithTooLongParamsRejected() {
        when(webhookDao.create(any(WebhookEntity.class))).thenReturn(webhook);
        var json = """
                {
                    "service_id": "some-service-id-that-is-way-toooooo-loooooooong",
                    "callback_url": "https://some-callback-url.com",
                    "live": true
                }
                """;
        var response = resources
                .target("/v1/webhook")
                .request()
                .post(Entity.json(json));

        assertThat(response.getStatus(), is(422));
    }
    
    @Test
    public void getWebhookByIdWhenWebhookExists() {
        when(webhookDao.findByExternalId(eq(existingWebhookId))).thenReturn(Optional.of(webhook));
        
        var response = resources
                .target("/v1/webhook/%s".formatted(existingWebhookId))
                .request()
                .get();

        assertThat(response.getStatus(), is(200));
    }
    
    @Test
    public void getWebhookByIdWhenDoesNotExist404() {
        when(webhookDao.findByExternalId(any(String.class))).thenReturn(Optional.empty());
        
        var response = resources
                .target("/v1/webhook/%s".formatted("aint_no_webhook"))
                .request()
                .get();

        assertThat(response.getStatus(), is(404));
    }
}

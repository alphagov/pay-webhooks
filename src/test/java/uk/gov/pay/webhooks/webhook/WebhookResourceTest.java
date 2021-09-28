package uk.gov.pay.webhooks.webhook;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.webhook.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.entity.dao.WebhookDao;

import javax.ws.rs.client.Entity;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DropwizardExtensionsSupport.class)
public class WebhookResourceTest {
   WebhookDao webhookDao = mock(WebhookDao.class);

    public final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new WebhookResource(webhookDao))
            .build();

    @Before
    public void setUp() {
        when(webhookDao.create(any(WebhookEntity.class))).thenReturn(1L);
    }

    @Test
    public void createWebhookWithValidParams() {
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
        var response = resources
                .target("/v1/webhook")
                .request()
                .post(Entity.json(""));

        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void createWebhookWithTooLongParamsRejected() {
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
}

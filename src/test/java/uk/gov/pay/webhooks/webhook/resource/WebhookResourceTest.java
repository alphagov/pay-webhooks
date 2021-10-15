package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.WebhookService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.ws.rs.client.Entity;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DropwizardExtensionsSupport.class)
public class WebhookResourceTest {
   WebhookService webhookService = mock(WebhookService.class);
   String existingWebhookId = "existing_webhook_id";
   String existingServiceId = "some-service-id";

    public final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new WebhookResource(webhookService))
            .build();
    
    WebhookEntity webhook;

    @BeforeEach
    void setup() {
        webhook = new WebhookEntity();
        webhook.setCreatedDate(Date.from(Instant.now()));
    }

    @Test
    public void createWebhookWithValidParams() {
        when(webhookService.createWebhook(any(CreateWebhookRequest.class))).thenReturn(webhook);
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
        when(webhookService.createWebhook(any(CreateWebhookRequest.class))).thenReturn(webhook);
        var response = resources
                .target("/v1/webhook")
                .request()
                .post(Entity.json(""));

        assertThat(response.getStatus(), is(422));
    }

    @Test
    public void createWebhookWithTooLongParamsRejected() {
        when(webhookService.createWebhook(any(CreateWebhookRequest.class))).thenReturn(webhook);
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
    public void createWebhookWithNonExistentEventTypeNameRejected() {
        when(webhookService.createWebhook(any(CreateWebhookRequest.class))).thenReturn(webhook);
        var json = """
                {
                    "service_id": "some-service-id",
                    "callback_url": "https://some-callback-url.com",
                    "live": true,
                    "subscriptions": ["nonexistent_event_type_name"]
                }
                """;
        var response = resources
                .target("/v1/webhook")
                .request()
                .post(Entity.json(json));

        assertThat(response.getStatus(), is(400));
    }
    
    @Test
    public void getWebhookByIdWhenWebhookExists() {
        when(webhookService.findByExternalId(eq(existingWebhookId), eq(existingServiceId))).thenReturn(Optional.of(webhook));
        
        var response = resources
                .target("/v1/webhook/%s".formatted(existingWebhookId))
                .queryParam("service_id", existingServiceId)
                .request()
                .get();

        assertThat(response.getStatus(), is(200));
    }    
    
    @Test
    public void getWebhookByIdWhenWebhookExistsAndServiceIdIncorrect404() {
        when(webhookService.findByExternalId(eq(existingWebhookId), eq(existingServiceId))).thenReturn(Optional.of(webhook));
        
        var response = resources
                .target("/v1/webhook/%s".formatted(existingWebhookId))
                .queryParam("service_id", "aint-no-serviceid")
                .request()
                .get();

        assertThat(response.getStatus(), is(404));
    }
    
    @Test
    public void getWebhookByIdWhenDoesNotExist404() {
        when(webhookService.findByExternalId(any(String.class), any(String.class))).thenReturn(Optional.empty());
        
        var response = resources
                .target("/v1/webhook/%s".formatted("aint_no_webhook"))
                .queryParam("service_id", "aint_no_service_id")
                .request()
                .get();

        assertThat(response.getStatus(), is(404));
    }    
    
    @Test
    public void getWebhookByIdWithoutServiceId400() {
        when(webhookService.findByExternalId(eq(existingWebhookId), any(String.class))).thenReturn(Optional.of(webhook));
        
        var response = resources
                .target("/v1/webhook/%s".formatted(existingWebhookId))
                .request()
                .get();

        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void getWebhooksReturnsListOfWebhooks() throws JsonProcessingException {
        webhook.setServiceId("some-service-id");
        webhook.setLive(true);
        webhook.setDescription("fooBar");
        webhook.addSubscription(new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED));
        webhook.setCreatedDate(Date.from(Instant.parse("2007-12-03T10:15:30.00Z")));

        when(webhookService.list(true, existingServiceId)).thenReturn((List.of(webhook, webhook)));

        var objectMapper = new ObjectMapper();
        var response = resources
                .target("/v1/webhook")
                .queryParam("live", true)
                .queryParam("service_id", existingServiceId)
                .request()
                .get();
        assertThat(response.getStatus(), is(200));
        JsonNode expected = objectMapper.readTree("""
                [{
                	"service_id": "some-service-id",
                	"live": true,
                	"callback_url": null,
                	"description": "fooBar",
                	"external_id": null,
                	"status": null,
                	"created_date": "2007-12-03T10:15:30.000Z",
                	"subscriptions": ["card_payment_captured"]
                }, {
                	"service_id": "some-service-id",
                	"live": true,
                	"callback_url": null,
                	"description": "fooBar",
                	"external_id": null,
                	"status": null,
                	"created_date": "2007-12-03T10:15:30.000Z",
                	"subscriptions": ["card_payment_captured"]
                }]
                """);
        JsonNode actual = objectMapper.readTree(response.readEntity(String.class));
        assertThat(actual, equalTo(expected));
    }

    @Test
    public void getWebhooksReturnsEmptyListIfNoResults() {
        when(webhookService.list(true, existingServiceId)).thenReturn((List.of()));
        
        var response = resources
                .target("/v1/webhook")
                .queryParam("live", true)
                .queryParam("service_id", existingServiceId)
                .request()
                .get();
        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), is("[]"));
    }    
    
    @Test
    public void getWebhooksRequestMissingParamsShould400() {
        when(webhookService.list(true, existingServiceId)).thenReturn((List.of()));
        
        var response = resources
                .target("/v1/webhook")
                .request()
                .get();
        assertThat(response.getStatus(), is(400));
    }    
    
    @Test
    public void getWebhooksRequestWithServiceIdAndOverrideShould400() throws JsonProcessingException {
        var response = resources
                .target("/v1/webhook")
                .queryParam("service_id", existingServiceId)
                .queryParam("live", true)
                .queryParam("override_service_id_restriction", true)
                .request()
                .get();
        var objectMapper = new ObjectMapper();
        Map<String, String> responseBody = objectMapper.readValue(response.readEntity(String.class), new TypeReference<>() {
        });
        assertThat(response.getStatus(), is(400));
        assertThat(responseBody.get("message"), is("service_id not permitted when using override_service_id_restriction"));
    }    
    
    @Test
    public void getWebhooksRequestWithLiveParamOnlyShould400() throws JsonProcessingException {
        var response = resources
                .target("/v1/webhook")
                .queryParam("live", true)
                .request()
                .get();
        var objectMapper = new ObjectMapper();
        Map<String, String> responseBody = objectMapper.readValue(response.readEntity(String.class), new TypeReference<>() {
        });
        assertThat(response.getStatus(), is(400));
        assertThat(responseBody.get("message"), is("either service_id or override_service_id_restriction query parameter must be provided"));
    }
}

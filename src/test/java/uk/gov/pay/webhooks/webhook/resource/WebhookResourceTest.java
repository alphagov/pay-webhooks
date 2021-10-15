package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.WebhookService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.ws.rs.client.Entity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
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
        
        
        when(webhookService.list(true, existingServiceId, null)).thenReturn((List.of(webhook, webhook)));
        
        var objectMapper = new ObjectMapper();
        var response = resources
                .target("/v1/webhook")
                .queryParam("live", true)
                .queryParam("service_id", existingServiceId)
                .request()
                .get();
        assertThat(response.getStatus(), is(200));

//        Exclude createdDate as that is dynamic
        var expectedResponse = objectMapper.readTree("""
                [{"service_id":"some-service-id",
                "live":true,
                "callback_url":null,
                "description":"fooBar",
                "external_id":null,
                "status":null,
                "subscriptions":["card_payment_captured"]},
                {"service_id":"some-service-id",
                "live":true,
                "callback_url":null,
                "description":"fooBar",
                "external_id":null,
                "status":null,
                "subscriptions":["card_payment_captured"]}]
                """);

        var jsonResponse = objectMapper.readTree(response.readEntity(String.class));
        assertThat(jsonResponse.size(), is(equalTo(expectedResponse.size())));
        for (final JsonNode objNode : jsonResponse) {
            assertThat(objNode.get("live"), equalTo(expectedResponse.get(0).get("live")));
            assertThat(objNode.get("description"), equalTo(expectedResponse.get(0).get("description")));
            assertThat(objNode.get("service_id"), equalTo(expectedResponse.get(0).get("service_id")));
            assertThat(objNode.get("subscriptions"), equalTo(expectedResponse.get(0).get("subscriptions")));
        }
    }    
    
    @Test
    public void getWebhooksReturnsEmptyListIfNoResults() {
        when(webhookService.list(true, existingServiceId, null)).thenReturn((List.of()));
        
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
        when(webhookService.list(true, existingServiceId, null)).thenReturn((List.of()));
        
        var response = resources
                .target("/v1/webhook")
                .request()
                .get();
        assertThat(response.getStatus(), is(400));
    }
}

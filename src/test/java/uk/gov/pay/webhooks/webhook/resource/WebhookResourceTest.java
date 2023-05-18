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
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.validations.CallbackUrlService;
import uk.gov.pay.webhooks.validations.WebhookRequestValidator;
import uk.gov.pay.webhooks.webhook.WebhookService;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;

import javax.ws.rs.client.Entity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(DropwizardExtensionsSupport.class)
public class WebhookResourceTest {
    public static final ObjectMapper objectMapper = new ObjectMapper();
    WebhookService webhookService = mock(WebhookService.class);
    WebhooksConfig webhooksConfig = mock(WebhooksConfig.class);
    WebhookRequestValidator webhookRequestValidator = new WebhookRequestValidator(new CallbackUrlService(webhooksConfig));
    String existingWebhookId = "existing_webhook_id";
    String existingServiceId = "some-service-id";

    public final ResourceExtension resources = ResourceExtension.builder()
            .addResource(new WebhookResource(webhookService, webhookRequestValidator))
            .build();

    WebhookEntity webhook;

    @BeforeEach
    void setup() {
        webhook = new WebhookEntity();
        webhook.setCreatedDate(Instant.now());
        when(webhooksConfig.getLiveDataAllowDomains()).thenReturn(Set.of("gov.uk"));
    }

    @Test
    public void createWebhookWithValidParams() {
        when(webhookService.createWebhook(any(CreateWebhookRequest.class))).thenReturn(webhook);
        var json = """
                {
                    "service_id": "some-service-id",
                    "gateway_account_id": "100",
                    "callback_url": "https://some-callback-url.com",
                    "live": false
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
                    "gateway_account_id": "100",
                    "callback_url": "https://some-callback-url.com",
                    "live": false
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
                    "gateway_account_id": "100",
                    "callback_url": "https://some-callback-url.com",
                    "live": false,
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
        when(webhookService.findByExternalIdAndServiceId(eq(existingWebhookId), eq(existingServiceId))).thenReturn(Optional.of(webhook));

        var response = resources
                .target("/v1/webhook/%s".formatted(existingWebhookId))
                .queryParam("service_id", existingServiceId)
                .request()
                .get();

        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void getWebhookByIdWhenWebhookExistsAndServiceIdIncorrect404() {
        when(webhookService.findByExternalIdAndServiceId(eq(existingWebhookId), eq(existingServiceId))).thenReturn(Optional.of(webhook));

        var response = resources
                .target("/v1/webhook/%s".formatted(existingWebhookId))
                .queryParam("service_id", "aint-no-serviceid")
                .request()
                .get();

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void getWebhookByIdWhenDoesNotExist404() {
        when(webhookService.findByExternalIdAndServiceId(any(String.class), any(String.class))).thenReturn(Optional.empty());

        var response = resources
                .target("/v1/webhook/%s".formatted("aint_no_webhook"))
                .queryParam("service_id", "aint_no_service_id")
                .request()
                .get();

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void getWebhookByIdWithoutServiceId400() {
        when(webhookService.findByExternalIdAndServiceId(eq(existingWebhookId), any(String.class))).thenReturn(Optional.of(webhook));

        var response = resources
                .target("/v1/webhook/%s".formatted(existingWebhookId))
                .request()
                .get();

        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void getWebhookByIdWithoutServiceIdWithOverrideFlagWhenWebhookExists() {
        when(webhookService.findByExternalId(eq(existingWebhookId))).thenReturn(Optional.of(webhook));

        var response = resources
                .target("/v1/webhook/%s".formatted(existingWebhookId))
                .queryParam("override_account_or_service_id_restriction", "true")
                .request()
                .get();

        assertThat(response.getStatus(), is(200)); 
    }

    @Test
    public void getWebhooksReturnsListOfWebhooks() throws JsonProcessingException {
        webhook.setServiceId("some-service-id");
        webhook.setGatewayAccountId("100");
        webhook.setLive(true);
        webhook.setDescription("fooBar");
        webhook.addSubscription(new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED));
        webhook.setCreatedDate(Instant.parse("2007-12-03T10:15:30.00Z"));

        when(webhookService.list(true, existingServiceId)).thenReturn((List.of(webhook, webhook)));

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
                	"gateway_account_id": "100",
                	"live": true,
                	"callback_url": null,
                	"description": "fooBar",
                	"external_id": null,
                	"status": null,
                	"created_date": "2007-12-03T10:15:30.000Z",
                	"subscriptions": ["card_payment_captured"]
                }, {
                	"service_id": "some-service-id",
                	"gateway_account_id": "100",
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
        Map<String, String> responseBody = objectMapper.readValue(response.readEntity(String.class), new TypeReference<>() {
        });
        assertThat(response.getStatus(), is(400));
        assertThat(responseBody.get("message"), is("either service_id or override_service_id_restriction query parameter must be provided"));
    }

    @Test
    void getWebhooksMessages_shouldReturn400WhenStatusIsNotValid() throws Exception {
        var response = resources
                .target("/v1/webhook/a-webhook-id/message")
                .queryParam("status", "INVALID")
                .request()
                .get();
        Map<String, String> responseBody = objectMapper.readValue(response.readEntity(String.class), new TypeReference<>() {
        });
        assertThat(response.getStatus(), is(400));
        assertThat(responseBody.get("message"), is("query param status must be one of [PENDING, SUCCESSFUL, FAILED, WILL_NOT_SEND]"));
    }
}

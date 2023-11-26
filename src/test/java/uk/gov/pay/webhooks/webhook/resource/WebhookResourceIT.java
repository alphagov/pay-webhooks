package uk.gov.pay.webhooks.webhook.resource;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.message.resource.WebhookMessageResponse;
import uk.gov.pay.webhooks.message.resource.WebhookMessageSearchResponse;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;

public class WebhookResourceIT {
    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension();
    private Integer port = app.getAppRule().getLocalPort();
    private DatabaseTestHelper dbHelper;
    
    @BeforeEach
    public void setUp() {
        dbHelper = DatabaseTestHelper.aDatabaseTestHelper(app.getJdbi());
        dbHelper.truncateAllWebhooksData();
    }

    @Test
    public void shouldCreateAndRetrieveAWebhook() {
        var json = createWebhookRequestBody("https://example.com", false);

        var response = given().port(port)
                .contentType(JSON)
                .body(json)
                .post("/v1/webhook")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("service_id", is("test_service_id"))
                .body("gateway_account_id", is("100"))
                .body("live", is(false))
                .body("callback_url", is("https://example.com"))
                .body("description", is("description"))
                .body("status", is("ACTIVE"))
                .body("subscriptions", containsInAnyOrder("card_payment_captured"))
                .extract()
                .as(Map.class);

        var externalId = response.get("external_id");
        var serviceId = response.get("service_id");
        var gatewayAccountId = response.get("gateway_account_id");

        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s?service_id=%s&gateway_account_id=%s".formatted(externalId, serviceId, gatewayAccountId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("service_id", is("test_service_id"))
                .body("gateway_account_id", is("100"))
                .body("live", is(false))
                .body("callback_url", is("https://example.com"))
                .body("description", is("description"))
                .body("status", is("ACTIVE"))
                .body("subscriptions", containsInAnyOrder("card_payment_captured"));
    }

    @Test
    public void shouldCreateRejectWebhookForKnownErrorIdentifiers() {
        given()
                .port(port)
                .contentType(JSON)
                .body(createWebhookRequestBody("http://gov.uk", true))
                .post("/v1/webhook")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("error_identifier", is("CALLBACK_URL_PROTOCOL_NOT_SUPPORTED"));
        given()
                .port(port)
                .contentType(JSON)
                .body(createWebhookRequestBody("http:/0/gov.uk", true))
                .post("/v1/webhook")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("error_identifier", is("CALLBACK_URL_MALFORMED"));
        given()
                .port(port)
                .contentType(JSON)
                .body(createWebhookRequestBody("https://gov.anotherdomain.com", true))
                .post("/v1/webhook")
                .then()
                .statusCode(Response.Status.BAD_REQUEST.getStatusCode())
                .body("error_identifier", is("CALLBACK_URL_NOT_ON_ALLOW_LIST"));
    }

    @Test
    public void shouldReturnFilteredMessages() {
        var externalId = "awebhookexternalid";
        var messageExternalId = "message-external-id-1";
        setupWebhookWithMessages(externalId,messageExternalId);

        given().port(port)
                .contentType(JSON)
                .queryParam("status", "FAILED")
                .queryParam("resource_id", "transaction-external-id")
                .get("/v1/webhook/%s/message".formatted(externalId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("count", is(1))
                .body("page", is(1))
                .body("results.last_delivery_status[0]", is("FAILED"))
                .body("results.latest_attempt[0].status", is("FAILED"));
    }

    @Test
    public void shouldReturnUnfilteredMessages() {
        var externalId = "awebhookexternalid";
        var messageExternalId = "message-external-id-1";
        setupWebhookWithMessages(externalId,messageExternalId);

        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s/message".formatted(externalId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("count", is(10))
                .body("page", is(1))
                .body("results.size()", is(10));
    }

    @Test
    public void shouldReturnPage2OfMessages() {
        var externalId = "awebhookexternalid";
        var messageExternalId = "message-external-id-1";
        setupWebhookWithMessages(externalId,messageExternalId);

        io.restassured.response.Response response = given().port(port)
                .contentType(JSON)
                .queryParam("page", 2)
                .get("/v1/webhook/%s/message".formatted(externalId));
        response
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("count", is(2))
                .body("page", is(2))
                .body("results.size()", is(2));
    }

    @Test
    public void messagesShouldReturn404ForMissingWebhook() {
        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/a-missing-webhook-id/message")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void shouldReturnAndCountEmptyMessages() {
        var externalId = "a-valid-webhook-id";
        dbHelper.addWebhook(externalId);
        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s/message".formatted(externalId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("count", is(0))
                .body("page", is(1))
                .body("results.size()", is(0));
    }

    @Test
    public void shouldReturnMessageAttempts() {
        var externalId = "awebhookexternalid";
        var messageExternalId = "message-external-id-1";
        setupWebhookWithMessages(externalId,messageExternalId);

        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s/message/%s/attempt".formatted(externalId, messageExternalId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("size()", is(3));
    }

    @Test
    public void shouldReturnMessageDetail() {
        var externalId = "awebhookexternalid";
        var messageExternalId = "message-external-id-1";
        setupWebhookWithMessages(externalId,messageExternalId);

        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s/message/%s".formatted(externalId, messageExternalId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("external_id", is(messageExternalId))
                .body("resource_id", is("transaction-external-id"))
                .body("resource_type", is("payment"))
                .body("last_delivery_status", is("FAILED"))
                .body("latest_attempt.status", is("FAILED"))
                .body("latest_attempt.response_time", is(25));
    }

    @Test
    public void shouldReturn404ForMessageNotFound() {
        var externalId = "awebhookexternalid";
        var messageExternalId = "message-external-id-1";
        setupWebhookWithMessages(externalId,messageExternalId);

        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s/message/a-missing-message-id".formatted(externalId))
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void notFoundShouldReturn404() {
        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/not-real-external-id?service_id=not-real-service-id&gateway_account_id=100")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    public void shouldDeleteSomeWebhookMessages() {
        var webhookExternalId = "a-webhook-external-id";
        WebhookMessageExternalIds webhookMessageExternalIds = setupWebhookWithMessagesExpectedToBePartiallyDeleted(webhookExternalId);
        List<String> expectedWebhookExternalIdsNotDeleted = webhookMessageExternalIds.notDeleted;
        List<String> expectedWebhookMessageExternalIds = setupThreeWebhookMessagesThatShouldNotBeDeleted(); // maxAgeOfMessages=7 so these webhook messages should not be deleted

        given().port(port)
                .contentType(JSON)
                .post("/v1/webhook/tasks/delete_messages")
                .then()
                .statusCode(200);

        WebhookMessageSearchResponse webhookMessageSearchResponse = given().port(port)
                .get(format("/v1/webhook/%s/message", webhookExternalId))
                .then()
                .body("results.size()", is(expectedWebhookExternalIdsNotDeleted.size() + expectedWebhookMessageExternalIds.size()))
                .extract().body().as(WebhookMessageSearchResponse.class);
        
        Collection<String> expectedWebhookExternalIds = 
                CollectionUtils.union(expectedWebhookMessageExternalIds, expectedWebhookExternalIdsNotDeleted);
        
        assertThat(expectedWebhookExternalIds,
                everyItem(in(webhookMessageSearchResponse.results().stream().map(WebhookMessageResponse::externalId).toList())));

        expectedWebhookExternalIds.forEach(webhookMessageExternalId ->
                given().port(port)
                        .get(format("/v1/webhook/%s/message/%s/attempt", webhookExternalId, webhookMessageExternalId))
                        .then()
                        .body("size()", is(1)));
        
        webhookMessageExternalIds.deleted.forEach(webhookMessageExternalId ->
                given().port(port)
                        .get(format("/v1/webhook/%s/message/%s", webhookExternalId, webhookMessageExternalId))
                        .then()
                        .statusCode(Response.Status.NOT_FOUND.getStatusCode()));
    }

    private List<String> setupThreeWebhookMessagesThatShouldNotBeDeleted() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String date = df.format(Date.from(OffsetDateTime.now().minusDays(1).toInstant()));
        List<String> webhookMessageExternalIds = List.of("thirteenth-message-external-id", "fourteenth-message-external-id", "fifteenth-message-external-id");
        dbHelper.addWebhookMessage(13, webhookMessageExternalIds.get(0), date, 1, date, 1, "{}", "transaction-external-id", "payment", "FAILED");
        dbHelper.addWebhookMessage(14, webhookMessageExternalIds.get(1), date, 1, date, 1, "{}", null, null, null);
        dbHelper.addWebhookMessage(15, webhookMessageExternalIds.get(2), date, 1, date, 1, "{}", null, null, null);
        dbHelper.addWebhookDeliveryQueueMessage(15, date, date, "200", "200", 13, "SUCCESSFUL", "1250");
        dbHelper.addWebhookDeliveryQueueMessage(16, date, date, "404", "404", 14, "FAILED", "25");
        dbHelper.addWebhookDeliveryQueueMessage(17, date, date, null, null, 15, "PENDING", null);
        return webhookMessageExternalIds;
    }

    private WebhookMessageExternalIds setupWebhookWithMessagesExpectedToBePartiallyDeleted(String externalId) {
        dbHelper.addWebhook(externalId);
        dbHelper.addWebhookMessage(1, "first-message-external-id", "2022-01-01", 1, "2022-01-01", 1,"{}", "transaction-external-id", "payment", "FAILED");
        //dbHelper.addWebhookMessages(1,10);
        dbHelper.addWebhookMessagesExpectedToBePartiallyDeleted();
        dbHelper.addWebhookDeliveryQueueMessage(1, "2022-01-01", "2022-01-01", "200", "200", 1, "SUCCESSFUL", "1250");
        dbHelper.addWebhookDeliveryQueueMessage(2, "2022-01-02", "2022-01-01", "404", "404", 1, "FAILED", "25");
        dbHelper.addWebhookDeliveryQueueMessage(3, "2022-01-02", "2022-01-01", null, null, 1, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(4, "2022-01-01", "2022-01-01", "404", "404", 2, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(5, "2022-01-01", "2022-01-01", "404", "404", 3, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(6, "2022-01-01", "2022-01-01", "404", "404", 4, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(7, "2022-01-01", "2022-01-01", "404", "404", 5, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(8, "2022-01-01", "2022-01-01", "404", "404", 6, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(9, "2022-01-01", "2022-01-01", "404", "404", 7, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(10, "2022-01-01", "2022-01-01", "404", "404", 8, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(11, "2022-01-01", "2022-01-01", "404", "404", 9, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(12, "2022-01-01", "2022-01-01", "404", "404", 10, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(13, "2022-01-01", "2022-01-01", "404", "404", 11, "PENDING", null);
        
        return new WebhookMessageExternalIds(
                List.of("first-message-external-id", "second-message-external-id", "third-message-external-id", "fourth-message-external-id", "fifth-message-external-id", "sixth-message-external-id"),
                List.of("seventh-message-external-id", "eighth-message-external-id", "ninth-message-external-id", "tenth-message-external-id", "eleventh-message-external-id")); // <-- Given maxNumOfMessagesToExpire=6, the webhook messages with these IDs won't be deleted
    }
    
    
    private record WebhookMessageExternalIds(List<String> deleted, List<String> notDeleted) {}

    private String createWebhookRequestBody(String callbackUrl, Boolean isLive) {
       return """
                {
                  "service_id": "test_service_id",
                  "gateway_account_id": "100",
                  "live": %s,
                  "callback_url": "%s",
                  "description": "description",
                  "subscriptions": ["card_payment_captured"]
                }
                """.formatted(isLive, callbackUrl);
    }

    private void setupWebhookWithMessages(String externalId, String messageExternalId) {
        dbHelper.addWebhook(externalId);
        dbHelper.addWebhookMessage(1, messageExternalId, "2022-01-01", 1, "2022-01-01", 1, "{}", "transaction-external-id", "payment", "FAILED");
        dbHelper.addWebhookMessage(2, "second-message-external-id", "2022-01-01", 1, "2022-01-01", 1, "{}", "transaction-external-id-2", "payment", "FAILED");
        //dbHelper.addWebhookMessages(3,10);
        dbHelper.addWebhookMessages();
        dbHelper.addWebhookDeliveryQueueMessage(1, "2022-01-01", "2022-01-01", "200", "200", 1, "SUCCESSFUL", "1250");
        dbHelper.addWebhookDeliveryQueueMessage(2, "2022-01-02", "2022-01-01", "404", "404", 1, "FAILED", "25");
        dbHelper.addWebhookDeliveryQueueMessage(3, "2022-01-02", "2022-01-01", null, null, 1, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(4, "2022-01-01", "2022-01-01", "404", "404", 2, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(5, "2022-01-01", "2022-01-01", "404", "404", 3, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(6, "2022-01-01", "2022-01-01", "404", "404", 4, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(7, "2022-01-01", "2022-01-01", "404", "404", 5, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(8, "2022-01-01", "2022-01-01", "404", "404", 6, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(9, "2022-01-01", "2022-01-01", "404", "404", 7, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(10, "2022-01-01", "2022-01-01", "404", "404", 8, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(11, "2022-01-01", "2022-01-01", "404", "404", 9, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(12, "2022-01-01", "2022-01-01", "404", "404", 10, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(13, "2022-01-01", "2022-01-01", "404", "404", 11, "PENDING", null);
        dbHelper.addWebhookDeliveryQueueMessage(14, "2022-01-01", "2022-01-01", "404", "404", 12, "PENDING", null);

    }
}

package uk.gov.pay.webhooks.webhook.resource;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
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
        dbHelper.truncateAllData();
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
                .body("live", is(false))
                .body("callback_url", is("https://example.com"))
                .body("description", is("description"))
                .body("status", is("ACTIVE"))
                .body("subscriptions", containsInAnyOrder("card_payment_captured"))
                .extract()
                .as(Map.class);

        var externalId = response.get("external_id");
        var serviceId = response.get("service_id");

        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s?service_id=%s".formatted(externalId, serviceId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("service_id", is("test_service_id"))
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
    public void shouldReturnMessages() {
        var externalId = "awebhookexternalid";
        var messageExternalId = "message-external-id-1";
        setupWebhookWithMessages(externalId, messageExternalId);

        given().port(port)
                .contentType(JSON)
                .queryParam("status", "FAILED")
                .get("/v1/webhook/%s/message".formatted(externalId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("results.last_delivery_status[0]", is("FAILED"))
                .body("results.latest_attempt[0].status", is("FAILED"));

        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s/message".formatted(externalId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("count", is(10))
                .body("page", is(1))
                .body("results.size()", is(10));

        given().port(port)
                .contentType(JSON)
                .queryParam("page", 2)
                .get("/v1/webhook/%s/message".formatted(externalId))
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
        app.getJdbi().withHandle(h -> h.execute("INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', 'service-id', true, 'http://callback-url.com', 'description', 'ACTIVE')".formatted(externalId)));

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
        setupWebhookWithMessages(externalId, messageExternalId);

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
        setupWebhookWithMessages(externalId, messageExternalId);

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
        setupWebhookWithMessages(externalId, messageExternalId);

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
                .get("/v1/webhook/not-real-external-id?service_id=not-real-service-id")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }
    
    @Test
    public void shouldDeleteNoWebhookMessagesAndSomeDeliveryAttempts() {
        var webhookExternalId = "a-webhook-external-id";
        String webhookMessageExternalId = setupOneWebhookMessageWithSevenDeliveryAttempts(webhookExternalId);

        given().port(port)
                .contentType(JSON)
                .post("/v1/webhook/tasks/expire_messages")
                .then()
                .statusCode(200);

        given().port(port)
                .get(format("/v1/webhook/%s/message", webhookExternalId))
                .then()
                .body("results.size()", is(1))
                .body("results[0].external_id", is(webhookMessageExternalId));

        given().port(port)
                .get(format("/v1/webhook/%s/message/%s/attempt", webhookExternalId, webhookMessageExternalId))
                .then()
                .body("size()", is(1)); // It's 1 because 7 delivery attempts exist and maxNumOfMessagesToExpire=6

    }

    private String setupOneWebhookMessageWithSevenDeliveryAttempts(String webhookExternalId) {
        app.getJdbi().withHandle(h -> h.execute(
                "INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', 'service-id', true, 'http://callback-url.com', 'description', 'ACTIVE')".formatted(webhookExternalId)
        ));
        app.getJdbi().withHandle(h -> h.execute("""
                            INSERT INTO webhook_messages VALUES
                            (1, 'first-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null)
                        """
        ));
        app.getJdbi().withHandle(h -> h.execute("""
                        INSERT INTO webhook_delivery_queue VALUES
                            (1, '2022-01-01', '2022-01-01', '200', 200, 1, 'SUCCESSFUL', 1250),
                            (2, '2022-01-02', '2022-01-01', '404', 404, 1, 'FAILED', 25),
                            (3, '2022-01-02', '2022-01-01', '200', 404, 1, 'FAILED', 25),
                            (4, '2022-01-02', '2022-01-01', '404', 404, 1, 'FAILED', 25),
                            (5, '2022-01-02', '2022-01-01', '200', 404, 1, 'FAILED', 25),
                            (6, '2022-01-02', '2022-01-01', '404', 404, 1, 'FAILED', 25),
                            (7, '2022-01-02', '2022-01-01', null, null, 1, 'PENDING', null)
                        """
        ));
        return "first-message-external-id";
    }

    @Test
    public void shouldDeleteSomeWebhookMessages() {
        var webhookExternalId = "a-webhook-external-id";
        List<String> expectedWebhookExternalIdsNotDeleted = setupWebhookWithMessagesExpectedToBePartiallyDeleted(webhookExternalId);
        List<String> expectedWebhookMessageExternalIds = setupThreeWebhookMessagesThatShouldNotBeDeleted(); // maxAgeOfMessages=7 so these webhook messages should not be deleted

        given().port(port)
                .contentType(JSON)
                .post("/v1/webhook/tasks/expire_messages")
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
    }

    private List<String> setupThreeWebhookMessagesThatShouldNotBeDeleted() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String date = df.format(Date.from(OffsetDateTime.now().minusDays(1).toInstant()));
        List<String> webhookMessageExternalIds = List.of("thirteenth-message-external-id", "fourteenth-message-external-id", "fifteenth-message-external-id");
        app.getJdbi().withHandle(h -> h.execute("""
                            INSERT INTO webhook_messages VALUES
                            (13, '%s', '%s', 1, '%s', 1, '{}', 'transaction-external-id', 'payment', 'FAILED'),
                            (14, '%s', '%s', 1, '%s', 1, '{}', null, null, null),
                            (15, '%s', '%s', 1, '%s', 1, '{}', null, null, null)
                        """.formatted(
                                webhookMessageExternalIds.get(0), date, date, 
                                webhookMessageExternalIds.get(1), date, date, 
                                webhookMessageExternalIds.get(2), date, date)
        ));
        app.getJdbi().withHandle(h -> h.execute("""
                        INSERT INTO webhook_delivery_queue VALUES
                            (15, '%s', '%s', '200', 200, 13, 'SUCCESSFUL', 1250),
                            (16, '%s', '%s', '404', 404, 14, 'FAILED', 25),
                            (17, '%s', '%s', null, null, 15, 'PENDING', null)
                        """.formatted(date, date, date, date, date, date)
        ));
        return webhookMessageExternalIds;
    }

    private List<String> setupWebhookWithMessagesExpectedToBePartiallyDeleted(String externalId) {
        app.getJdbi().withHandle(h -> h.execute(
                "INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', 'service-id', true, 'http://callback-url.com', 'description', 'ACTIVE')".formatted(externalId)
        ));
        app.getJdbi().withHandle(h -> h.execute("""
                            INSERT INTO webhook_messages VALUES
                            (1, 'first-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', 'transaction-external-id', 'payment', 'FAILED'),
                            (2, 'second-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (3, 'third-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (4, 'fourth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (5, 'fifth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (6, 'sixth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (7, 'seventh-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (8, 'eighth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (9, 'ninth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (10, 'tenth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (11, 'eleventh-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null)
                        """
        ));
        app.getJdbi().withHandle(h -> h.execute("""
                        INSERT INTO webhook_delivery_queue VALUES
                            (1, '2022-01-01', '2022-01-01', '200', 200, 1, 'SUCCESSFUL', 1250),
                            (2, '2022-01-02', '2022-01-01', '404', 404, 1, 'FAILED', 25),
                            (3, '2022-01-02', '2022-01-01', null, null, 1, 'PENDING', null),
                            (4, '2022-01-01', '2022-01-01', '404', 404, 2, 'PENDING', null),
                            (5, '2022-01-01', '2022-01-01', '404', 404, 3, 'PENDING', null),
                            (6, '2022-01-01', '2022-01-01', '404', 404, 4, 'PENDING', null),
                            (7, '2022-01-01', '2022-01-01', '404', 404, 5, 'PENDING', null),
                            (8, '2022-01-01', '2022-01-01', '404', 404, 6, 'PENDING', null),
                            (9, '2022-01-01', '2022-01-01', '404', 404, 7, 'PENDING', null),
                            (10, '2022-01-01', '2022-01-01', '404', 404, 8, 'PENDING', null),
                            (11, '2022-01-01', '2022-01-01', '404', 404, 9, 'PENDING', null),
                            (12, '2022-01-01', '2022-01-01', '404', 404, 10, 'PENDING', null),
                            (13, '2022-01-01', '2022-01-01', '404', 404, 11, 'PENDING', null)
                        """
        ));
        // Given maxNumOfMessagesToExpire=6, the first six webhook_delivery_queue entries will be deleted, leaving the
        // seven which correspond to the following webhook_messages externalIds:
        return List.of("fifth-message-external-id", "sixth-message-external-id", "seventh-message-external-id", 
                "eighth-message-external-id", "ninth-message-external-id", "tenth-message-external-id", 
                "eleventh-message-external-id");
    }

    private String createWebhookRequestBody(String callbackUrl, Boolean isLive) {
       return """
                {
                  "service_id": "test_service_id",
                  "live": %s,
                  "callback_url": "%s",
                  "description": "description",
                  "subscriptions": ["card_payment_captured"]
                }
                """.formatted(isLive, callbackUrl);
    }

    private void setupWebhookWithMessages(String externalId, String messageExternalId) {
        app.getJdbi().withHandle(h -> h.execute(
                "INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', 'service-id', true, 'http://callback-url.com', 'description', 'ACTIVE')".formatted(externalId)
        ));
        app.getJdbi().withHandle(h -> h.execute("""
                            INSERT INTO webhook_messages VALUES
                            (1, '%s', '2022-01-01', 1, '2022-01-01', 1, '{}', 'transaction-external-id', 'payment', 'FAILED'),
                            (2, 'second-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (3, 'third-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (4, 'fourth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (5, 'fifth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (6, 'sixth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (7, 'seventh-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (8, 'eighth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (9, 'ninth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (10, 'tenth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (11, 'eleventh-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null),
                            (12, 'twelfth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}', null, null, null)
                        """.formatted(messageExternalId)
        ));
        app.getJdbi().withHandle(h -> h.execute("""
                        INSERT INTO webhook_delivery_queue VALUES
                            (1, '2022-01-01', '2022-01-01', '200', 200, 1, 'SUCCESSFUL', 1250),
                            (2, '2022-01-02', '2022-01-01', '404', 404, 1, 'FAILED', 25),
                            (3, '2022-01-02', '2022-01-01', null, null, 1, 'PENDING', null),
                            (4, '2022-01-01', '2022-01-01', '404', 404, 2, 'PENDING', null),
                            (5, '2022-01-01', '2022-01-01', '404', 404, 3, 'PENDING', null),
                            (6, '2022-01-01', '2022-01-01', '404', 404, 4, 'PENDING', null),
                            (7, '2022-01-01', '2022-01-01', '404', 404, 5, 'PENDING', null),
                            (8, '2022-01-01', '2022-01-01', '404', 404, 6, 'PENDING', null),
                            (9, '2022-01-01', '2022-01-01', '404', 404, 7, 'PENDING', null),
                            (10, '2022-01-01', '2022-01-01', '404', 404, 8, 'PENDING', null),
                            (11, '2022-01-01', '2022-01-01', '404', 404, 9, 'PENDING', null),
                            (12, '2022-01-01', '2022-01-01', '404', 404, 10, 'PENDING', null),
                            (13, '2022-01-01', '2022-01-01', '404', 404, 11, 'PENDING', null),
                            (14, '2022-01-01', '2022-01-01', '404', 404, 12, 'PENDING', null)
                        """
        ));
    }
}

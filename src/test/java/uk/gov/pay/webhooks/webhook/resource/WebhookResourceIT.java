package uk.gov.pay.webhooks.webhook.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;


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
        var json = """
                {
                  "service_id": "test_service_id",
                  "live": true,
                  "callback_url": "https://example.com",
                  "description": "description",
                  "subscriptions": ["card_payment_captured"]
                }
                """;

        var response = given().port(port)
                .contentType(JSON)
                .body(json)
                .post("/v1/webhook")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("service_id", is("test_service_id"))
                .body("live", is(true))
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
                .body("live", is(true))
                .body("callback_url", is("https://example.com"))
                .body("description", is("description"))
                .body("status", is("ACTIVE"))
                .body("subscriptions", containsInAnyOrder("card_payment_captured"));
    }

    @Test
    public void shouldReturnMessages() {
        var externalId = "awebhookexternalid";
        var messageExternalId = "message-external-id-1";
        setupWebhookWithMessages(externalId, messageExternalId);

        given().port(port)
                .contentType(JSON)
                .queryParam("status", "FAILED")
                .get("/v1/webhook/%s/messages".formatted(externalId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("results.latest_attempt[0].status", is("FAILED"));

        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s/messages".formatted(externalId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("count", is(10))
                .body("total", is(12))
                .body("page", is(1))
                .body("results.size()", is(10));

        given().port(port)
                .contentType(JSON)
                .queryParam("page", 2)
                .get("/v1/webhook/%s/messages".formatted(externalId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("count", is(2))
                .body("total", is(12))
                .body("page", is(2))
                .body("results.size()", is(2));
    }

    @Test
    public void shouldReturnMessageAttempts() {
        var externalId = "awebhookexternalid";
        var messageExternalId = "message-external-id-1";
        setupWebhookWithMessages(externalId, messageExternalId);

        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s/messages/%s/attempts".formatted(externalId, messageExternalId))
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body("size()", is(3));
    }

    @Test
    public void notFoundShouldReturn404() {
        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/not-real-external-id?service_id=not-real-service-id")
                .then()
                .statusCode(Response.Status.NOT_FOUND.getStatusCode());
    }

    private String setupWebhookWithMessages(String externalId, String messageExternalId) {
        app.getJdbi().withHandle(h -> h.execute(
                "INSERT INTO webhooks VALUES (1, '2022-01-01', '%s', 'signing-key', 'service-id', true, 'http://callback-url.com', 'description', 'ACTIVE')".formatted(externalId)
        ));
        app.getJdbi().withHandle(h -> h.execute("""
                            INSERT INTO webhook_messages VALUES
                            (1, '%s', '2022-01-01', 1, '2022-01-01', 1, '{}'),
                            (2, 'second-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}'),
                            (3, 'third-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}'),
                            (4, 'fourth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}'),
                            (5, 'fifth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}'),
                            (6, 'sixth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}'),
                            (7, 'seventh-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}'),
                            (8, 'eighth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}'),
                            (9, 'ninth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}'),
                            (10, 'tenth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}'),
                            (11, 'eleventh-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}'),
                            (12, 'twelfth-message-external-id', '2022-01-01', 1, '2022-01-01', 1, '{}')
                        """.formatted(messageExternalId)
        ));
        app.getJdbi().withHandle(h -> h.execute("""
                        INSERT INTO webhook_delivery_queue VALUES
                            (1, '2022-01-01', '2022-01-01', '200', 200, 1, 'SUCCESSFUL'),
                            (2, '2022-01-02', '2022-01-01', '404', 404, 1, 'FAILED'),
                            (3, '2022-01-02', '2022-01-01', null, null, 1, 'PENDING'),
                            (4, '2022-01-01', '2022-01-01', '404', 404, 2, 'PENDING'),
                            (5, '2022-01-01', '2022-01-01', '404', 404, 3, 'PENDING'),
                            (6, '2022-01-01', '2022-01-01', '404', 404, 4, 'PENDING'),
                            (7, '2022-01-01', '2022-01-01', '404', 404, 5, 'PENDING'),
                            (8, '2022-01-01', '2022-01-01', '404', 404, 6, 'PENDING'),
                            (9, '2022-01-01', '2022-01-01', '404', 404, 7, 'PENDING'),
                            (10, '2022-01-01', '2022-01-01', '404', 404, 8, 'PENDING'),
                            (11, '2022-01-01', '2022-01-01', '404', 404, 9, 'PENDING'),
                            (12, '2022-01-01', '2022-01-01', '404', 404, 10, 'PENDING'),
                            (13, '2022-01-01', '2022-01-01', '404', 404, 11, 'PENDING'),
                            (14, '2022-01-01', '2022-01-01', '404', 404, 12, 'PENDING')
                        """
        ));
        return externalId;
    }
}

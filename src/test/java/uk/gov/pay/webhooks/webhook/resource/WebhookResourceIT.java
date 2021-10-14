package uk.gov.pay.webhooks.webhook.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresExtension;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;
import uk.gov.pay.webhooks.webhook.dao.WebhookDao;

import javax.ws.rs.core.Response;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;


public class WebhookResourceIT {
    @RegisterExtension
    public static AppWithPostgresExtension app = new AppWithPostgresExtension();
    private Integer port = app.getAppRule().getLocalPort();
    private DatabaseTestHelper dbHelper;



    @BeforeEach
    public void setUp() {
        dbHelper = dbHelper.aDatabaseTestHelper(app.getJdbi());
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
}

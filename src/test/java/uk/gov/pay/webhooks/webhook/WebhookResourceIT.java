package uk.gov.pay.webhooks.webhook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresExtension;

import javax.ws.rs.core.Response;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;


public class WebhookResourceIT {
    @RegisterExtension
    public static AppWithPostgresExtension app = new AppWithPostgresExtension();
    private Integer port = app.getAppRule().getLocalPort();

    @Test
    public void shouldCreateAWebhook() {
        var json = """
                {
                  "service_id": "test_service_id",
                  "live": true,
                  "callback_url": "https://example.com",
                  "description": "description"
                }
                """;

        given().port(port)
                .contentType(JSON)
                .body(json)
                .post("/v1/webhook")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }
}

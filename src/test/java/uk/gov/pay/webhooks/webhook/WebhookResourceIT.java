package uk.gov.pay.webhooks.webhook;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresExtension;
import uk.gov.service.payments.commons.model.Source;

import javax.ws.rs.core.Response;

import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static io.restassured.RestAssured.given;


public class WebhookResourceIT {
    @RegisterExtension
    public static AppWithPostgresExtension app = new AppWithPostgresExtension();
    private Integer port = app.getAppRule().getLocalPort();

    @Test
    public void shouldCreateAWebhook() {
        var json = """
                {
                  "key": "value"
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

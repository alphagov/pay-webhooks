package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresExtension;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

import java.util.Map;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;

public class WebhookUpdateIT {
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
    public void shouldUpdateDescription() throws JsonProcessingException {
        var json = """
                {
                  "service_id": "test_service_id",
                  "live": true,
                  "callback_url": "https://example.com",
                  "description": "original description",
                  "subscriptions": ["card_payment_captured"]
                }
                """;

        var response = given().port(port)
                .contentType(JSON)
                .body(json)
                .post("/v1/webhook")
                .then()
                .extract()
                .as(Map.class);

        var payload = singletonList(Map.of("path", "description",
                "op", "replace",
                "value", "new description"));

        var externalId = response.get("external_id");
        var serviceId = response.get("service_id");

        var mapper = new ObjectMapper();
        given().port(port)
                .contentType(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format("/v1/webhook/%s?service_id=%s", externalId, serviceId))
                .then()
                .statusCode(200)
                .body("description", is("new description"));

        given().port(port)
                .contentType(JSON)
                .get(format("/v1/webhook/%s?service_id=%s", externalId, serviceId))
                .then()
                .statusCode(200)
                .body("description", is("new description"));
    }
    
    
    
    
}

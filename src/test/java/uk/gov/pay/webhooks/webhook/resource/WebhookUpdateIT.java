package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;

public class WebhookUpdateIT {
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
    public void shouldUpdateDescriptionAndCallbackUrl() {
        var json = """
                {
                  "service_id": "test_service_id",
                  "gateway_account_id": "100",
                  "live": false,
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

        var payload = """
                    [{
                            "path": "description",
                            "op": "replace",
                            "value": "new description"
                        },
                        {
                            "path": "callback_url",
                            "op": "replace",
                            "value": "https://example.org"
                        }
                    ]
                """;
        var externalId = response.get("external_id");
        var serviceId = response.get("service_id");
        var gatewayAccountId = response.get("gateway_account_id");
        
        given().port(port)
                .contentType(JSON)
                .body(payload)
                .patch(format("/v1/webhook/%s?service_id=%s&gateway_account_id=%s", externalId, serviceId, gatewayAccountId))
                .then()
                .statusCode(200)
                .body("description", is("new description"))
                .body("callback_url", is("https://example.org"));

        given().port(port)
                .contentType(JSON)
                .get(format("/v1/webhook/%s?service_id=%s&gateway_account_id=%s", externalId, serviceId, gatewayAccountId))
                .then()
                .statusCode(200)
                .body("description", is("new description"))
                .body("callback_url", is("https://example.org"));
    }

    @Test
    public void shouldMakeWebhookInactive() throws JsonProcessingException {
        var json = """
                {
                  "service_id": "test_service_id",
                  "gateway_account_id": "100",
                  "live": false,
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

        var payload = singletonList(Map.of(
                "path", "status",
                "op", "replace",
                "value", "INACTIVE"));

        var externalId = response.get("external_id");
        var serviceId = response.get("service_id");
        var gatewayAccountId = response.get("gateway_account_id");

        var mapper = new ObjectMapper();
        given().port(port)
                .contentType(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format("/v1/webhook/%s?service_id=%s&gateway_account_id=%s", externalId, serviceId, gatewayAccountId))
                .then()
                .statusCode(200)
                .body("status", is("INACTIVE"));
    }

    @Test
    public void shouldUpdateSubscriptions() {
        var json = """
                {
                  "service_id": "test_service_id",
                  "gateway_account_id": "100",
                  "live": false,
                  "callback_url": "https://example.com",
                  "description": "original description",
                  "subscriptions": []
                }
                """;

        var response = given().port(port)
                .contentType(JSON)
                .body(json)
                .post("/v1/webhook")
                .then()
                .extract()
                .as(Map.class);

        var payload = """
                    [{
                        "path": "subscriptions",
                        "op": "replace",
                        "value": ["card_payment_captured"]
                    }]
                """;

        var externalId = response.get("external_id");
        var serviceId = response.get("service_id");
        var gatewayAccountId = response.get("gateway_account_id");

        given().port(port)
                .contentType(JSON)
                .body(payload)
                .patch(format("/v1/webhook/%s?service_id=%s&gateway_account_id=%s", externalId, serviceId, gatewayAccountId))
                .then()
                .statusCode(200)
                .body("subscriptions", containsInAnyOrder("card_payment_captured"));
    }

    @Test
    public void shouldRejectCallbackUrlUpdateWithAppropriateErrorIdentifier() throws JsonProcessingException {
        var json = """
                {
                  "service_id": "test_service_id",
                  "gateway_account_id": "100",
                  "live": true,
                  "callback_url": "https://gov.uk",
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

        var payload = singletonList(Map.of(
                "path", "callback_url",
                "op", "replace",
                "value", "https://notgov.uk"));

        var externalId = response.get("external_id");
        var serviceId = response.get("service_id");
        var gatewayAccountId = response.get("gateway_account_id");

        var mapper = new ObjectMapper();
        given().port(port)
                .contentType(JSON)
                .body(mapper.writeValueAsString(payload))
                .patch(format("/v1/webhook/%s?service_id=%s&gateway_account_id=%s", externalId, serviceId, gatewayAccountId))
                .then()
                .statusCode(400)
                .body("error_identifier", is("CALLBACK_URL_NOT_ON_ALLOW_LIST"));
    }
}

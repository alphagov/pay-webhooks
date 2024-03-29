package uk.gov.pay.webhooks.webhook.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

public class WebhookSigningKeyIT {
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
    public void shouldGetThenRegenerateSigningKey() {
        var json = """
                {
                  "service_id": "test_service_id",
                  "gateway_account_id": "100",
                  "live": false,
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
                .statusCode(200)
                .extract()
                .as(Map.class);

        var externalId = response.get("external_id");
        var serviceId = response.get("service_id");
        var gatewayAccountId = response.get("gateway_account_id");
        
        var signingKeyResponse = given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s/signing-key?service_id=%s&gateway_account_id=%s".formatted(externalId, serviceId, gatewayAccountId))
                .then()
                .statusCode(200)
                .body("signing_key", startsWith("webhook_test_"))
                .extract()
                .as(Map.class);

        var originalSigningKey = signingKeyResponse.get("signing_key");
        assertThat(originalSigningKey.toString().length(), allOf(lessThanOrEqualTo(39), greaterThan(30))); //occasionally a shorter string is generated

        var regeneratePostResponse = given().port(port)
                .contentType(JSON)
                .post("/v1/webhook/%s/signing-key?service_id=%s&gateway_account_id=%s".formatted(externalId, serviceId, gatewayAccountId))
                .then()
                .statusCode(200)
                .body("signing_key", not(originalSigningKey))
                .body("signing_key", startsWith("webhook_test_"))
                .extract()
                .as(Map.class);

        var regeneratedSigningKey = regeneratePostResponse.get("signing_key");
        assertThat(regeneratedSigningKey.toString().length(), allOf(lessThanOrEqualTo(39), greaterThan(30))); //occasionally a shorter string is generated

        given().port(port)
                .contentType(JSON)
                .get("/v1/webhook/%s/signing-key?service_id=%s&gateway_account_id=%s".formatted(externalId, serviceId, gatewayAccountId))
                .then()
                .statusCode(200)
                .body("signing_key", equalTo(regeneratedSigningKey));
    }
}

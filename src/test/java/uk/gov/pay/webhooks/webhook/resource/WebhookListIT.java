package uk.gov.pay.webhooks.webhook.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.webhooks.util.DatabaseTestHelper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;

public class WebhookListIT {
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
    public void shouldRetrieveWebhookList() {
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
                .extract()
                .as(Map.class);

        var externalId = response.get("external_id");
        var serviceId = response.get("service_id");

        var listResponse = given().port(port)
                .contentType(JSON)
                .get("/v1/webhook?service_id=test_service_id&live=true".formatted(externalId, serviceId))
                .then()
                .extract().as(List.class);
            assertThat(listResponse.size(),is(equalTo(1)));
            var firstItem = (LinkedHashMap) listResponse.get(0);
            assertThat(firstItem.get("service_id"), is("test_service_id"));
    }

    @Test
    public void shouldRetrieveWebhookListByLiveStatus() {
        var serviceOne = """
                {
                  "service_id": "service_one",
                  "live": true,
                  "callback_url": "https://example.com",
                  "description": "description",
                  "subscriptions": ["card_payment_captured"]
                }
                """;

        var serviceTwo = """
                {
                  "service_id": "service_two",
                  "live": true,
                  "callback_url": "https://example.com",
                  "description": "description",
                  "subscriptions": ["card_payment_captured"]
                }
                """;
        
        List.of(serviceOne, serviceTwo).forEach(service ->
                        given().port(port)
                                .contentType(JSON)
                                .body(service)
                                .post("/v1/webhook")
                );
        
        var jsonNodeResponse = given().port(port)
                .contentType(JSON)
                .get("/v1/webhook?override_service_id_restriction=true&live=true")
                .then()
                .extract().as(JsonNode.class);
        
        assertThat(jsonNodeResponse.size(),is(equalTo(2)));
        assertThat(jsonNodeResponse.findValues("service_id"), 
                containsInAnyOrder(new TextNode("service_one"), new TextNode("service_two")));

    }
}


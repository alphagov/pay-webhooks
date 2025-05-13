package uk.gov.pay.webhooks.healthcheck;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import uk.gov.pay.extension.AppWithPostgresAndSqsExtension;
import uk.gov.pay.rule.PostgresTestDocker;
import uk.gov.pay.rule.SqsTestDocker;

import static org.hamcrest.Matchers.equalTo;

class HealthCheckResourceIT {
    @RegisterExtension
    public static AppWithPostgresAndSqsExtension app = new AppWithPostgresAndSqsExtension();

    @Test
    void HealthCheckIsHealthyTest() {
        RestAssured.given().port(app.getAppRule().getLocalPort())
                .contentType(ContentType.JSON)
                .when()
                .accept(ContentType.JSON)
                .get("healthcheck")
                .then()
                .statusCode(200)
                .body("database.healthy", equalTo(true))
                .body("deadlocks.healthy", equalTo(true))
                .body("hibernate.healthy", equalTo(true))
                .body("ping.healthy", equalTo(true))
                .body("sqsQueue.healthy", equalTo(true));
    }

    @Test
    void healthCheckShouldReturn503WhenDBAndSqsQueueDown() throws InterruptedException {
        PostgresTestDocker.shutDown();
        DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
                .queueUrl(SqsTestDocker.getQueueUrl("event-queue"))
                .build();
        app.getSqsClient().deleteQueue(deleteQueueRequest);

        RestAssured.given().port(app.getAppRule().getLocalPort())
                .contentType(ContentType.JSON)
                .when()
                .accept(ContentType.JSON)
                .get("healthcheck")
                .then()
                .statusCode(503)
                .body("database.healthy", equalTo(false))
                .body("deadlocks.healthy", equalTo(true))
                .body("hibernate.healthy", equalTo(false))
                .body("ping.healthy", equalTo(true))
                .body("sqsQueue.healthy", equalTo(false));
    }
}

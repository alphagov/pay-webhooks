package uk.gov.pay.webhooks.healthcheck;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import uk.gov.pay.extension.AppWithPostgresExtension;

import static org.hamcrest.Matchers.equalTo;

public class HealthCheckResourceIT {
    @RegisterExtension
    public static AppWithPostgresExtension app = new AppWithPostgresExtension();

    @Test
    public void HealthCheckIsHealthyTest(){
        RestAssured.given().port(app.getAppRule().getLocalPort())
                .contentType(ContentType.JSON)
                .when()
                .accept(ContentType.JSON)
                .get("healthcheck")
                .then()
                .statusCode(200)
                .body("[0].database.healthy", equalTo(true))
                .body("[1].deadlocks.healthy", equalTo(true))
                .body("[2].hibernate.healthy", equalTo(true))
                .body("[3].ping.healthy", equalTo(true));
    }

}

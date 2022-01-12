package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.queue.InternalEvent;

import java.time.Instant;
import java.time.InstantSource;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@ExtendWith(DropwizardExtensionsSupport.class)
class WebhookMessageTest {
        
    private ObjectMapper objectMapper;
    private InstantSource instantSource;

    @BeforeEach
    public void setUp() {
        instantSource = InstantSource.fixed(Instant.from(Date.from(Instant.parse("2019-10-01T08:25:24.00Z")).toInstant()));
        objectMapper = new ObjectMapper();

    }

    @Test
    void serialisesWebhookMessageBody() throws JsonProcessingException {
        String resource = """
            {
                "json": "and",
                "the": "argonauts"
            }
            """;
        var webhookMessageEntity = new WebhookMessageEntity();
        webhookMessageEntity.setExternalId("externalId");

        var internalEvent = new InternalEvent("PAYMENT_CAPTURED", "service-id", true, "resource-external-id", null, instantSource.instant(), "payment");
        var body = WebhookMessage.of(webhookMessageEntity, internalEvent, objectMapper.readTree(resource));;
        var expectedJson = """
          {
            "created_date": "2019-10-01T08:25:24.000Z",
            "resource_id": "resource-external-id",
            "api_version": 1,
            "resource_type": "payment",
            "event_type": "PAYMENT_CAPTURED",
            "id": "externalId",
            "resource": {
                "json": "and",
                "the": "argonauts"
            }
          }
                """;

        assertThat(objectMapper.readTree(expectedJson), equalTo(objectMapper.valueToTree(body)));

    }
}

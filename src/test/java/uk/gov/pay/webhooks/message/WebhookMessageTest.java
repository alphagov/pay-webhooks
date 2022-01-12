package uk.gov.pay.webhooks.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.gov.pay.webhooks.eventtype.EventTypeName;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
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
        webhookMessageEntity.setEventDate(Date.from(instantSource.instant()));
        EventTypeEntity eventTypeEntity = new EventTypeEntity(EventTypeName.CARD_PAYMENT_CAPTURED);
        webhookMessageEntity.setEventType(eventTypeEntity);
        webhookMessageEntity.setResourceType("payment");
        webhookMessageEntity.setResourceExternalId("resource-external-id");
        webhookMessageEntity.setResource(objectMapper.readTree(resource));
        
        var body = WebhookMessage.of(webhookMessageEntity);;
        var expectedJson = """
                {
                 	"id": "externalId",
                 	"created_date": "2019-10-01T08:25:24.000Z",
                 	"resource_id": "resource-external-id",
                 	"api_version": 1,
                 	"resource_type": "payment",
                 	"event_type_name": "card_payment_captured",
                 	"resource": {
                 		"json": "and",
                 		"the": "argonauts"
                 	}
                 }
                """;

        assertThat(objectMapper.readTree(expectedJson), equalTo(objectMapper.valueToTree(body)));

    }
}

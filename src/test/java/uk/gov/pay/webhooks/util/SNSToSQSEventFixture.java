package uk.gov.pay.webhooks.util;

import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.Optional;

public class SNSToSQSEventFixture {
    private JsonNode body;
    private final ObjectNode fixture = (ObjectNode) Jackson.getObjectMapper().readTree(getClass().getResource("/sns-to-sqs-event.json"));
    public SNSToSQSEventFixture() throws IOException {
    }

    public static SNSToSQSEventFixture anSNSToSQSEventFixture() throws IOException {
        return new SNSToSQSEventFixture();
    }

    public String build() {
        getBody().map(JsonNode::toString).map(body -> fixture.put("Message", body));
        return fixture.toString();
    }

    public SNSToSQSEventFixture withBody(Object body) {
        this.body = Jackson.getObjectMapper().valueToTree(body);
        return this;
    }

    public Optional<JsonNode> getBody() {
        return Optional.ofNullable(body);
    }
}

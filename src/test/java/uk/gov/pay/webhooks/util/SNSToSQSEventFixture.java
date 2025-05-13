package uk.gov.pay.webhooks.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

public class SNSToSQSEventFixture {
    private JsonNode body;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectNode fixture;
    public SNSToSQSEventFixture() throws IOException {
        URL json = getClass().getResource("/sns-to-sqs-event.json");
        this.fixture = (ObjectNode) objectMapper.readTree(json);
    }

    public static SNSToSQSEventFixture anSNSToSQSEventFixture() throws IOException {
        return new SNSToSQSEventFixture();
    }

    public String build() {
        getBody().map(JsonNode::toString).map(body -> fixture.put("Message", body));
        return fixture.toString();
    }

    public SNSToSQSEventFixture withBody(Object body) {
        this.body = objectMapper.valueToTree(body);
        return this;
    }

    public Optional<JsonNode> getBody() {
        return Optional.ofNullable(body);
    }
}

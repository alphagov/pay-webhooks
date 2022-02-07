package uk.gov.pay.webhooks.util;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstantDeserializerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private record Entity(@JsonDeserialize(using = InstantDeserializer.class) Instant instant) { }

    @BeforeEach
    public void setUp() {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addDeserializer(Instant.class, new InstantDeserializer());
        objectMapper.registerModule(simpleModule);
    }

    @Test
    void shouldDeserializeValidInstantString() throws IOException {
        var timestamp = "2022-01-02T17:21:12.123456789Z";

        var json = """
                {
                    "instant": "%s"
                }
                """.formatted(timestamp);

        assertThat(objectMapper.readValue(json, Entity.class).instant(), is(Instant.parse(timestamp)));
    }

    @Test
    void shouldDeserializeNullToNull() throws IOException {
        var json = """
                {
                    "instant": null
                }
                """;

        assertThat(objectMapper.readValue(json, Entity.class).instant(), is(nullValue()));
    }

    @Test
    void shouldThrowExceptionWhenValueInvalid() {
        var json = """
                {
                    "instant": "invalid"
                }
                """;

        assertThrows(JsonMappingException.class, () -> objectMapper.readValue(json, Entity.class));
    }

}

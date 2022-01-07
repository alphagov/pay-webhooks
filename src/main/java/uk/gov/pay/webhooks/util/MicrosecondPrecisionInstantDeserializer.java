package uk.gov.pay.webhooks.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class MicrosecondPrecisionInstantDeserializer extends JsonDeserializer<Instant> {

    public static final DateTimeFormatter MICROSECOND_FORMATTER =
            MicrosecondPrecisionInstantSerializer.MICROSECOND_FORMATTER;
    

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt)  {
        try {
            return Instant.from(MICROSECOND_FORMATTER.parse(p.getText()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
}

package uk.gov.pay.webhooks.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

public class MicrosecondPrecisionInstantSerializer extends JsonSerializer<Instant> {

    public static final DateTimeFormatter MICROSECOND_FORMATTER =
            new DateTimeFormatterBuilder()
                    .appendInstant(6)
                    .toFormatter(Locale.ENGLISH)
                    .withZone(ZoneOffset.UTC);
    
    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(MICROSECOND_FORMATTER.format(value));  
    }
}


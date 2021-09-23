package uk.gov.pay.webhooks.webhook.converter;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Converts an Instant to a LocalDateTime using UTC as the assumed local time
 * zone. Per JDBC 4.2, this LocalDateTime can then be stored in a database
 * column of type TIMESTAMP WITHOUT TIME ZONE.
 *
 * Alternatively, takes a LocalDateTime read from a database column of type
 * TIMESTAMP WITHOUT TIME ZONE and converts it into an Instant by assuming it
 * refers to a date and time in the UTC time zone.
 **/
@Converter
public class InstantToUtcTimestampWithoutTimeZoneJpaConverter implements AttributeConverter<Instant, LocalDateTime> {

    @Override
    public LocalDateTime convertToDatabaseColumn(Instant instant) {
        if (instant == null) {
            return null;
        }

        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    @Override
    public Instant convertToEntityAttribute(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }

        return localDateTime.toInstant(ZoneOffset.UTC);
    }

}

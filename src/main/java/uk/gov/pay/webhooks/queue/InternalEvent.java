package uk.gov.pay.webhooks.queue;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeSerializer;

import java.time.ZonedDateTime;

public record InternalEvent(
        String eventType,
        String serviceId,
        Boolean live,
        String resourceExternalId,
        @JsonSerialize(using = MicrosecondPrecisionDateTimeSerializer.class) ZonedDateTime eventDate,
        String eventData
        ) {}

package uk.gov.pay.webhooks.queue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.webhooks.util.MicrosecondPrecisionInstantSerializer;

import java.time.Instant;

public record InternalEvent(
        String eventType,
        String serviceId,
        Boolean live,
        String resourceExternalId,
        String parentResourceExternalId,
        JsonNode eventData,
        @JsonSerialize(using = MicrosecondPrecisionInstantSerializer.class) Instant eventDate,
        String resourceType
        ) {}

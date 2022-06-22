package uk.gov.pay.webhooks.queue;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.pay.webhooks.util.MicrosecondPrecisionInstantSerializer;

import java.time.Instant;

public record InternalEvent(
        String eventType,
        String serviceId,
        Boolean live,
        String resourceExternalId,
        String parentResourceExternalId,
        @JsonSerialize(using = MicrosecondPrecisionInstantSerializer.class) Instant timestamp,
        String resourceType
        ) {}

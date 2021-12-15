package uk.gov.pay.webhooks.queue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.service.payments.commons.api.json.MicrosecondPrecisionDateTimeDeserializer;

import java.time.ZonedDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EventMessageDto(String serviceId,
                              Boolean live,
                              @JsonDeserialize(using = MicrosecondPrecisionDateTimeDeserializer.class) ZonedDateTime timestamp,
                              @JsonProperty("resource_external_id") String resourceExternalId,
                              String eventType,
                              @JsonProperty("event_details") JsonNode eventDetails
) {}

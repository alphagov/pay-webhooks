package uk.gov.pay.webhooks.queue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.webhooks.util.InstantDeserializer;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record EventMessageDto(@JsonProperty("service_id") String serviceId,
                              Boolean live,
                              @JsonProperty("event_date") @JsonDeserialize(using = InstantDeserializer.class) Instant eventDate,
                              @JsonProperty("resource_external_id") String resourceExternalId,
                              @JsonProperty("parent_resource_external_id") String parentResourceExternalId,
                              @JsonProperty("event_type") String eventType,
                              @JsonProperty("resource_type") String resourceType,
                              @JsonProperty("event_data") JsonNode eventData
) {}

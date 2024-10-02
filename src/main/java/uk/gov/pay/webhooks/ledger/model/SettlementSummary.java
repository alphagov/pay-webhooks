package uk.gov.pay.webhooks.ledger.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.pay.webhooks.util.InstantDeserializer;

import java.time.Instant;

import static uk.gov.service.payments.commons.model.CommonDateTimeFormatters.ISO_INSTANT_MILLISECOND_PRECISION;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettlementSummary {

    @JsonDeserialize(using = InstantDeserializer.class)
    @JsonProperty("capture_submit_time")
    private Instant captureSubmitTime;
    @JsonProperty("captured_date")
    private String capturedDate;

    public String getCaptureSubmitTime() {
        return (captureSubmitTime != null) ? ISO_INSTANT_MILLISECOND_PRECISION.format(captureSubmitTime) : null;
    }

    public void setCaptureSubmitTime(Instant captureSubmitTime) {
        this.captureSubmitTime = captureSubmitTime;
    }

    public String getCapturedDate() {
        return capturedDate;
    }

    public void setCapturedDate(String capturedDate) {
        this.capturedDate = capturedDate;
    }

}

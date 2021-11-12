package uk.gov.pay.webhooks.ledger.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import uk.gov.service.payments.commons.api.json.ApiResponseDateTimeDeserializer;

import java.time.ZonedDateTime;

import static uk.gov.service.payments.commons.model.ApiResponseDateTimeFormatter.ISO_INSTANT_MILLISECOND_PRECISION;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SettlementSummary {

    @JsonDeserialize(using = ApiResponseDateTimeDeserializer.class)
    @JsonProperty("capture_submit_time")
    private ZonedDateTime captureSubmitTime;
    @JsonProperty("captured_date")
    private String capturedDate;

    public String getCaptureSubmitTime() {
        return (captureSubmitTime != null) ? ISO_INSTANT_MILLISECOND_PRECISION.format(captureSubmitTime) : null;
    }

    public void setCaptureSubmitTime(ZonedDateTime captureSubmitTime) {
        this.captureSubmitTime = captureSubmitTime;
    }

    public String getCapturedDate() {
        return capturedDate;
    }

    public void setCapturedDate(String capturedDate) {
        this.capturedDate = capturedDate;
    }

}

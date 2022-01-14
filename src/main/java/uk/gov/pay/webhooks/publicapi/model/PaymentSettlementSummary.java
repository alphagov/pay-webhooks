package uk.gov.pay.webhooks.publicapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentSettlementSummary {

    @JsonProperty("capture_submit_time")
    private String captureSubmitTime;

    @JsonProperty("captured_date")
    private String capturedDate;

    @JsonProperty("settled_date")
    private String settledDate;

    public PaymentSettlementSummary() {}

    public PaymentSettlementSummary(String captureSubmitTime, String capturedDate, String settledDate) {
        this.captureSubmitTime = captureSubmitTime;
        this.capturedDate = capturedDate;
        this.settledDate = settledDate;
    }
    
    public String getCaptureSubmitTime() {
        return captureSubmitTime;
    }

    public String getCapturedDate() {
        return capturedDate;
    }

    public String getSettledDate() {
        return settledDate;
    }
}

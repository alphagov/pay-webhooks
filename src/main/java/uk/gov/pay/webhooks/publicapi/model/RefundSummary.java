package uk.gov.pay.webhooks.publicapi.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundSummary {

    private String status;

    @JsonProperty("amount_available")
    private long amountAvailable;

    @JsonProperty("amount_submitted")
    private long amountSubmitted;

    public RefundSummary() {}

    public RefundSummary(String status, long amountAvailable, long amountSubmitted) {
        this.status = status;
        this.amountAvailable = amountAvailable;
        this.amountSubmitted = amountSubmitted;
    }

    public String getStatus() {
        return status;
    }

    public long getAmountAvailable() {
        return amountAvailable;
    }

    public long getAmountSubmitted() {
        return amountSubmitted;
    }
}

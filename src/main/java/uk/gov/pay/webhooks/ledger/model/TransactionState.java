package uk.gov.pay.webhooks.ledger.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionState {

    private String status;

    public TransactionState() {
    }

    public TransactionState(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

}

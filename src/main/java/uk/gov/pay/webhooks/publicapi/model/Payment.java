package uk.gov.pay.webhooks.publicapi.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)

public abstract class Payment {
    public static final String LINKS_JSON_ATTRIBUTE = "_links";

    @JsonProperty("payment_id")
    protected String paymentId;

    @JsonProperty("payment_provider")
    protected String paymentProvider;

    protected long amount;
    protected String description;
    protected String reference;


    @JsonProperty("created_date")
    protected String createdDate;

    protected Payment() {
        //To enable Jackson serialisation we need a default constructor
    }

    public Payment(String chargeId,
                   long amount,
                   String description,
                   String reference,
                   String paymentProvider,
                   String createdDate) {
        this.paymentId = chargeId;
        this.amount = amount;
        this.description = description;
        this.reference = reference;
        this.paymentProvider = paymentProvider;
        this.createdDate = createdDate;
    }
    
    public String getCreatedDate() {
        return createdDate;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public long getAmount() {
        return amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getReference() {
        return reference;
    }
    
    public String getPaymentProvider() {
        return paymentProvider;
    }
}

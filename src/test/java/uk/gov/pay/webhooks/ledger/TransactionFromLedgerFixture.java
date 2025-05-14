package uk.gov.pay.webhooks.ledger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.webhooks.ledger.model.TransactionState;

// src/test/resources/pacts/webhooks-ledger-get-payment-transaction.json
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionFromLedgerFixture {
    private final int gatewayAccountId = 3;
    private final int amount = 12000;
    private final TransactionState state = new TransactionState("success");
    private final String description = "New passport application";
    private final String reference = "1_86";
    private final String language = "cy";
    private final String returnUrl = "https://service-name.gov.uk/transactions/12345";
    private final String email = "Joe.Bogs@example.org";
    private final String paymentProvider = "sandbox";
    private final String credentialExternalId = "credential-external-id";
    private final String createdDate = "2020-02-13T16:26:04.204Z";
    private final Boolean delayedCapture = false;
    private final Boolean moto = false;
    private final Boolean live = false;
    private final String transactionId = "e8eq11mi2ndmauvb51qsg8hccn";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public TransactionFromLedgerFixture() {
    }

    public static TransactionFromLedgerFixture aTransactionFromLedgerFixture() {
        return new TransactionFromLedgerFixture();
    }

    public String build() throws JsonProcessingException {
        return objectMapper.writeValueAsString(this);
    }

    public String getTransactionId() {
        return transactionId;
    }

    public int getGatewayAccountId() {
        return gatewayAccountId;
    }

    public int getAmount() {
        return amount;
    }

    public TransactionState getState() {
        return state;
    }

    public String getDescription() {
        return description;
    }

    public String getReference() {
        return reference;
    }

    public String getLanguage() {
        return language;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public String getCredentialExternalId() {
        return credentialExternalId;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public Boolean getDelayedCapture() {
        return delayedCapture;
    }

    public Boolean getMoto() {
        return moto;
    }

    public Boolean getLive() {
        return live;
    }
}

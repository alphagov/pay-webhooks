package uk.gov.pay.webhooks.ledger;

import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.webhooks.ledger.model.TransactionState;

// src/test/resources/pacts/webhooks-ledger-get-payment-transaction.json
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TransactionFromLedgerFixture {
    private final int gatewayAccountId = 3;
    private final int amount = 12000;
    private final TransactionState transactionState = new TransactionState("success");
    private final String description = "New passport application";
    private final String reference = "1_86";
    private final String language = "cy";
    private final String returnUrl = "https://service-name.gov.uk/transactions/12345";
    private final String email = "Joe.Bogs@example.org";
    private final String paymentProvider = "sandbox";
    private final String credentialExternalId = "credential-external-id";
    private final String createdDate = "2020-02-13T16:26:04.204Z";
    private final Boolean delayedCapture = false;
    private final String transactionType = "PAYMENT";
    private final Boolean moto = false;
    private final Boolean live = false;
    private final String transactionId = "e8eq11mi2ndmauvb51qsg8hccn";

    public TransactionFromLedgerFixture() {
    }

    public static TransactionFromLedgerFixture aTransactionFromLedgerFixture() {
        return new TransactionFromLedgerFixture();
    }

    public String build() throws JsonProcessingException {
        return Jackson.getObjectMapper().writeValueAsString(this);
    }
}

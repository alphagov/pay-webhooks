package uk.gov.pay.webhooks.message.apirepresentation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;
import uk.gov.pay.webhooks.ledger.model.SettlementSummary;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RefundApiRepresentation(String refundId,
                                      String createdDate,
                                      Long amount,
                                      String status,
                                      SettlementSummary settlementSummary,
                                      String paymentId) {
    public static RefundApiRepresentation of(LedgerTransaction ledgerTransaction) {
     return new RefundApiRepresentation(ledgerTransaction.getTransactionId(),
             ledgerTransaction.getCreatedDate(),
             ledgerTransaction.getAmount(),
             ledgerTransaction.getState().getStatus(),
             ledgerTransaction.getSettlementSummary(),
             ledgerTransaction.getParentTransactionId());   
    }
}

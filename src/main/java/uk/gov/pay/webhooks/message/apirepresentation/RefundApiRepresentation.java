package uk.gov.pay.webhooks.message.apirepresentation;

import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;
import uk.gov.pay.webhooks.ledger.model.SettlementSummary;

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

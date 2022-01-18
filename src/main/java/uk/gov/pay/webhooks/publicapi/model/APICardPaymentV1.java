package uk.gov.pay.webhooks.publicapi.model;

import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;

import java.util.Optional;

public class APICardPaymentV1 {
    public static PaymentForSearchResult paymentForSearchResultFrom(LedgerTransaction ledgerTransaction) {
        var externalChargeState = ExternalChargeState.fromStatusString(ledgerTransaction.getState().getStatus());
        return new PaymentForSearchResult(
                ledgerTransaction.getTransactionId(),
                ledgerTransaction.getAmount(),
                new PaymentState(externalChargeState.getStatus(), externalChargeState.isFinished(), externalChargeState.getMessage(), externalChargeState.getCode()),
                ledgerTransaction.getReturnUrl(),
                ledgerTransaction.getDescription(),
                ledgerTransaction.getReference(),
                ledgerTransaction.getEmail(),
                ledgerTransaction.getPaymentProvider(),
                ledgerTransaction.getCreatedDate(),
                ledgerTransaction.getLanguage(),
                ledgerTransaction.getDelayedCapture(),
                ledgerTransaction.isMoto(),
                Optional.ofNullable(ledgerTransaction.getRefundSummary()).map(rs -> new RefundSummary(rs.getStatus(), rs.getAmountAvailable(), rs.getAmountSubmitted())).orElse(null),
                Optional.ofNullable(ledgerTransaction.getSettlementSummary()).map(ss -> new PaymentSettlementSummary(ss.getCaptureSubmitTime(), ss.getCapturedDate(), null)).orElse(null),
                new CardDetails(ledgerTransaction.getCardDetails().getLastDigitsCardNumber(),
                        ledgerTransaction.getCardDetails().getFirstDigitsCardNumber(),
                        ledgerTransaction.getCardDetails().getCardholderName(),
                        ledgerTransaction.getCardDetails().getExpiryDate(),
                        new Address(ledgerTransaction.getCardDetails().getBillingAddress().getLine1(),
                                ledgerTransaction.getCardDetails().getBillingAddress().getLine2(),
                                ledgerTransaction.getCardDetails().getBillingAddress().getPostcode(),
                                ledgerTransaction.getCardDetails().getBillingAddress().getCity(),
                                ledgerTransaction.getCardDetails().getBillingAddress().getCountry()),
                        ledgerTransaction.getCardDetails().getCardBrand(),
                        ledgerTransaction.getCardDetails().getCardType()),
                ledgerTransaction.getCorporateCardSurcharge(),
                ledgerTransaction.getTotalAmount(),
                ledgerTransaction.getPaymentProvider(),
                Optional.ofNullable(ledgerTransaction.getExternalMetaData()).map(ExternalMetadata::new).orElse(null),
                ledgerTransaction.getFee(),
                ledgerTransaction.getNetAmount(),
                new AuthorisationSummary(new ThreeDSecure(ledgerTransaction.getAuthorisationSummary().getThreeDSecure().isRequired())));
    }
}

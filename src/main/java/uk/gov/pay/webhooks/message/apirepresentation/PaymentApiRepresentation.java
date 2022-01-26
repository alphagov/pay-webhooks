package uk.gov.pay.webhooks.message.apirepresentation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;
import uk.gov.pay.webhooks.ledger.model.SettlementSummary;

import java.util.Map;
import java.util.Optional;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaymentApiRepresentation(
        String paymentId,
        Long amount,
        PaymentState state,
        String returnUrl,
        String description,
        String reference,
        String email,
        String paymentProvider,
        String createdDate,
        String language,
        boolean delayedCapture,
        boolean moto,
        PaymentApiRefundSummary refundSummary,
        SettlementSummary settlementSummary,
        PaymentApiCardDetails cardDetails,
        Long corporateCardSurcharge,
        Long totalAmount,
        Map<String, Object> metadata,
        Long fee,
        Long netAmount,
        PaymentApiAuthorisationSummary authorisationSummary
) {

    public static PaymentApiRepresentation of(LedgerTransaction ledgerTransaction) {
        var externalChargeState = ExternalChargeState.fromStatusString(ledgerTransaction.getState().getStatus());
        return new PaymentApiRepresentation(ledgerTransaction.getTransactionId(),
                ledgerTransaction.getAmount(),
                new PaymentState(externalChargeState.getStatus(), externalChargeState.isFinished(), externalChargeState.getMessage(), externalChargeState.getCode()),
                ledgerTransaction.getReturnUrl(),
                ledgerTransaction.getDescription(),
                ledgerTransaction.getReference(),
                ledgerTransaction.getEmail(),
                ledgerTransaction.getPaymentProvider(),
                ledgerTransaction.getCreatedDate(),
                ledgerTransaction.getLanguage().toString(),
                ledgerTransaction.getDelayedCapture(),
                ledgerTransaction.isMoto(),
                Optional.ofNullable(ledgerTransaction.getRefundSummary()).map(rs -> new PaymentApiRefundSummary(rs.getStatus(), rs.getAmountAvailable(), rs.getAmountSubmitted())).orElse(null),
                ledgerTransaction.getSettlementSummary(),
                Optional.ofNullable(ledgerTransaction.getCardDetails()).map(cd -> new PaymentApiCardDetails(cd.getLastDigitsCardNumber(), cd.getFirstDigitsCardNumber(),cd.getCardholderName(), cd.getExpiryDate(),
                        Optional.ofNullable(ledgerTransaction.getCardDetails().getBillingAddress()).map(ba -> new PaymentApiAddress(ba.getLine1(), ba.getLine2(), ba.getPostcode(), ba.getCity(), ba.getCountry())).orElse(null),
                        cd.getCardBrand(),
                        cd.getCardType())).orElse(null),
                ledgerTransaction.getCorporateCardSurcharge(),
                ledgerTransaction.getTotalAmount(),
                ledgerTransaction.getExternalMetaData(),
                ledgerTransaction.getFee(),
                ledgerTransaction.getNetAmount(),
                new PaymentApiAuthorisationSummary(new PaymentApiThreeDSecure(ledgerTransaction.getAuthorisationSummary().getThreeDSecure().isRequired()))
        );
    }
}

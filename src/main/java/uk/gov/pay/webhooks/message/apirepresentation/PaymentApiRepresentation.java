package uk.gov.pay.webhooks.message.apirepresentation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import uk.gov.pay.webhooks.ledger.model.CardDetails;
import uk.gov.pay.webhooks.ledger.model.LedgerTransaction;
import uk.gov.pay.webhooks.ledger.model.RefundSummary;
import uk.gov.pay.webhooks.ledger.model.SettlementSummary;
import uk.gov.pay.webhooks.ledger.model.TransactionState;
import uk.gov.service.payments.commons.model.SupportedLanguage;

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
        @JsonSerialize(using = ToStringSerializer.class) SupportedLanguage language,
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

    private static PaymentApiRefundSummary ofRefundSummary(RefundSummary rs) {
        return new PaymentApiRefundSummary(rs.getStatus(), rs.getAmountAvailable(), rs.getAmountSubmitted());
    }
    
    private static PaymentApiCardDetails ofCardDetails(CardDetails cd) {
        return new PaymentApiCardDetails(cd.getLastDigitsCardNumber(), cd.getFirstDigitsCardNumber(),cd.getCardholderName(), cd.getExpiryDate(),
                Optional.ofNullable(cd.getBillingAddress()).map(ba -> new PaymentApiAddress(ba.getLine1(), ba.getLine2(), ba.getPostcode(), ba.getCity(), ba.getCountry())).orElse(null),
                cd.getCardBrand(),
                cd.getCardType());
    } 
    
    private static PaymentState getPaymentState(TransactionState ts) {
        var externalChargeState = ExternalChargeState.fromStatusString(ts.getStatus());
        return new PaymentState(externalChargeState.getStatus(), externalChargeState.isFinished(), externalChargeState.getMessage(), externalChargeState.getCode());
    }
   
    public static PaymentApiRepresentation of(LedgerTransaction ledgerTransaction) {
        return new PaymentApiRepresentation(ledgerTransaction.getTransactionId(),
                ledgerTransaction.getAmount(),
                getPaymentState(ledgerTransaction.getState()),
                ledgerTransaction.getReturnUrl(),
                ledgerTransaction.getDescription(),
                ledgerTransaction.getReference(),
                ledgerTransaction.getEmail(),
                ledgerTransaction.getPaymentProvider(),
                ledgerTransaction.getCreatedDate(),
                ledgerTransaction.getLanguage(),
                ledgerTransaction.getDelayedCapture(),
                ledgerTransaction.isMoto(),
                Optional.ofNullable(ledgerTransaction.getRefundSummary()).map(PaymentApiRepresentation::ofRefundSummary).orElse(null),
                ledgerTransaction.getSettlementSummary(),
                Optional.ofNullable(ledgerTransaction.getCardDetails()).map(PaymentApiRepresentation::ofCardDetails).orElse(null),
                ledgerTransaction.getCorporateCardSurcharge(),
                ledgerTransaction.getTotalAmount(),
                ledgerTransaction.getExternalMetaData(),
                ledgerTransaction.getFee(),
                ledgerTransaction.getNetAmount(),
                new PaymentApiAuthorisationSummary(new PaymentApiThreeDSecure(ledgerTransaction.getAuthorisationSummary().getThreeDSecure().isRequired()))
        );
    }
}

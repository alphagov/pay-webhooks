package uk.gov.pay.webhooks.publicapi.model;

import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;


public class PaymentForSearchResult extends CardPayment {



    public PaymentForSearchResult(String chargeId, long amount, PaymentState state, String returnUrl, String description,
                                  String reference, String email, String paymentProvider, String createdDate, SupportedLanguage language,
                                  boolean delayedCapture, boolean moto, RefundSummary refundSummary, PaymentSettlementSummary settlementSummary, CardDetails cardDetails,
                                  Long corporateCardSurcharge, Long totalAmount, String providerId, ExternalMetadata externalMetadata,
                                  Long fee, Long netAmount, AuthorisationSummary authorisationSummary) {

        super(chargeId, amount, state, returnUrl, description, reference, email, paymentProvider,
                createdDate, refundSummary, settlementSummary, cardDetails, language, delayedCapture, moto, corporateCardSurcharge, totalAmount, providerId, externalMetadata,
                fee, netAmount, authorisationSummary);
        
    }

    public static PaymentForSearchResult valueOf(
            TransactionResponse paymentResult) {

        return new PaymentForSearchResult(
                paymentResult.getTransactionId(),
                paymentResult.getAmount(),
                paymentResult.getState(),
                paymentResult.getReturnUrl(),
                paymentResult.getDescription(),
                paymentResult.getReference(),
                paymentResult.getEmail(),
                paymentResult.getPaymentProvider(),
                paymentResult.getCreatedDate(),
                paymentResult.getLanguage(),
                paymentResult.getDelayedCapture(),
                paymentResult.isMoto(),
                paymentResult.getRefundSummary(),
                paymentResult.getSettlementSummary(),
                paymentResult.getCardDetails(),
                paymentResult.getCorporateCardSurcharge(),
                paymentResult.getTotalAmount(),
                paymentResult.getGatewayTransactionId(),
                paymentResult.getMetadata().orElse(null),
                paymentResult.getFee(),
                paymentResult.getNetAmount(),
                paymentResult.getAuthorisationSummary());
    }
    
}



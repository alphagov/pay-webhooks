package uk.gov.pay.webhooks.publicapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import uk.gov.service.payments.commons.api.json.ExternalMetadataSerialiser;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.model.charge.ExternalMetadata;

import java.util.Optional;

import static uk.gov.service.payments.commons.model.TokenPaymentType.CARD;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class CardPayment extends Payment {

    @JsonProperty("refund_summary")
    private final RefundSummary refundSummary;

    @JsonProperty("settlement_summary")
    private final PaymentSettlementSummary settlementSummary;

    @JsonProperty("card_details")
    private final CardDetails cardDetails;

    @JsonSerialize(using = ToStringSerializer.class)
    private final SupportedLanguage language;

    @JsonProperty("delayed_capture")
    private final boolean delayedCapture;

    @JsonProperty("moto")
    private final boolean moto;

    @JsonProperty("corporate_card_surcharge")
    private final Long corporateCardSurcharge;

    @JsonProperty("total_amount")
    private final Long totalAmount;

    @JsonProperty("fee")
    private final Long fee;

    @JsonProperty("net_amount")
    private final Long netAmount;

    @JsonProperty("provider_id")
    private final String providerId;

    @JsonSerialize(using = ExternalMetadataSerialiser.class)
    private final ExternalMetadata metadata;

    @JsonProperty("return_url")
    protected String returnUrl;

    protected String email;

    protected PaymentState state;

    //Used by Swagger to document the right model in the PaymentsResource
    @JsonIgnore
    protected String paymentType;

    @JsonProperty("authorisation_summary")
    private AuthorisationSummary authorisationSummary;


    public CardPayment(String chargeId, long amount, PaymentState state, String returnUrl, String description,
                       String reference, String email, String paymentProvider, String createdDate,
                       RefundSummary refundSummary, PaymentSettlementSummary settlementSummary, CardDetails cardDetails,
                       SupportedLanguage language, boolean delayedCapture, boolean moto, Long corporateCardSurcharge, Long totalAmount,
                       String providerId, ExternalMetadata metadata, Long fee, Long netAmount, AuthorisationSummary authorisationSummary) {
        super(chargeId, amount, description, reference, paymentProvider, createdDate);
        this.state = state;
        this.refundSummary = refundSummary;
        this.settlementSummary = settlementSummary;
        this.cardDetails = cardDetails;
        this.providerId = providerId;
        this.metadata = metadata;
        this.paymentType = CARD.getFriendlyName();
        this.language = language;
        this.delayedCapture = delayedCapture;
        this.moto = moto;
        this.corporateCardSurcharge = corporateCardSurcharge;
        this.totalAmount = totalAmount;
        this.fee = fee;
        this.netAmount = netAmount;
        this.email = email;
        this.returnUrl = returnUrl;
        this.authorisationSummary = authorisationSummary;
    }

    /**
     * card brand is no longer a top level charge property. It is now at `card_details.card_brand` attribute
     * We still need to support `v1` clients with a top level card brand attribute to keep support their integrations.
     *
     * @return
     */

    @JsonProperty("card_brand")
    @Deprecated
    public String getCardBrand() {
        return cardDetails != null ? cardDetails.getCardBrand() : null;
    }

    public ExternalMetadata getMetadata() {
        return metadata;
    }

    public Optional<RefundSummary> getRefundSummary() {
        return Optional.ofNullable(refundSummary);
    }

    public Optional<PaymentSettlementSummary> getSettlementSummary() {
        return Optional.ofNullable(settlementSummary);
    }

    public Optional<CardDetails> getCardDetails() {
        return Optional.ofNullable(cardDetails);
    }

    public SupportedLanguage getLanguage() {
        return language;
    }

    public boolean getDelayedCapture() {
        return delayedCapture;
    }

    public boolean getMoto() { return moto; }

    public Optional<Long> getCorporateCardSurcharge() {
        return Optional.ofNullable(corporateCardSurcharge);
    }

    public Optional<Long> getFee() {
        return Optional.ofNullable(fee);
    }

    public Optional<Long> getNetAmount() {
        return Optional.ofNullable(netAmount);
    }

    public Optional<Long> getTotalAmount() {
        return Optional.ofNullable(totalAmount);
    }

    public String getProviderId() {
        return providerId;
    }

    public Optional<String> getReturnUrl() {
        return Optional.ofNullable(returnUrl);
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    public PaymentState getState() {
        return state;
    }

    public AuthorisationSummary getAuthorisationSummary() {
        return authorisationSummary;
    }

    @Override
    public String toString() {
        // Don't include:
        // description - some services include PII
        // reference - can come from user input for payment links, in the past they have mistakenly entered card numbers
        return "Card Payment{" +
                "paymentId='" + super.paymentId + '\'' +
                ", paymentProvider='" + paymentProvider + '\'' +
                ", cardBrandLabel='" + getCardBrand() + '\'' +
                ", amount=" + amount +
                ", fee=" + fee +
                ", netAmount=" + netAmount +
                ", corporateCardSurcharge='" + corporateCardSurcharge + '\'' +
                ", state='" + state + '\'' +
                ", returnUrl='" + returnUrl + '\'' +
                ", language='" + language.toString() + '\'' +
                ", delayedCapture=" + delayedCapture +
                ", moto=" + moto +
                ", createdDate='" + createdDate + '\'' +
                '}';
    }
}

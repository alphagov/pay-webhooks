package uk.gov.pay.webhooks.ledger.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import uk.gov.service.payments.commons.model.Source;
import uk.gov.service.payments.commons.model.SupportedLanguage;
import uk.gov.service.payments.commons.model.SupportedLanguageJsonDeserializer;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LedgerTransaction {

    private String transactionId;
    private Long amount;
    private String gatewayAccountId;
    private String credentialExternalId;
    private String description;
    private String reference;
    private String email;
    private boolean delayedCapture;
    private Long corporateCardSurcharge;
    private Long totalAmount;
    private Long fee;
    private Long netAmount;
    private String createdDate;
    private String gatewayTransactionId;
    private TransactionState state;
    private String returnUrl;
    private String paymentProvider;
    private RefundSummary refundSummary;
    private SettlementSummary settlementSummary;
    private CardDetails cardDetails;
    @JsonDeserialize(using = SupportedLanguageJsonDeserializer.class)
    private SupportedLanguage language;
    private boolean moto;
    private Boolean live;
    private Source source;
    private String walletType;
    @JsonProperty("metadata")
    private Map<String, Object> externalMetaData;
    private String refundedBy;
    private String refundedByUserEmail;
    private String parentTransactionId;
    private String serviceId;
    private String transactionType;
    private AuthorisationSummary authorisationSummary;

    public LedgerTransaction() {

    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean getDelayedCapture() {
        return delayedCapture;
    }

    public Long getCorporateCardSurcharge() {
        return corporateCardSurcharge;
    }

    public void setCorporateCardSurcharge(Long corporateCardSurcharge) {
        this.corporateCardSurcharge = corporateCardSurcharge;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getFee() {
        return fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }

    public Long getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Long netAmount) {
        this.netAmount = netAmount;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getCredentialExternalId() {
        return credentialExternalId;
    }

    public void setCredentialExternalId(String credentialExternalId) {
        this.credentialExternalId = credentialExternalId;
    }

    public TransactionState getState() {
        return state;
    }

    public void setState(TransactionState transactionState) {
        this.state = transactionState;
    }

    public String getGatewayAccountId() {
        return gatewayAccountId;
    }

    public void setGatewayAccountId(String gatewayAccountId) {
        this.gatewayAccountId = gatewayAccountId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public boolean isDelayedCapture() {
        return delayedCapture;
    }

    public void setDelayedCapture(boolean delayedCapture) {
        this.delayedCapture = delayedCapture;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(String paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public RefundSummary getRefundSummary() {
        return refundSummary;
    }

    public void setRefundSummary(RefundSummary refundSummary) {
        this.refundSummary = refundSummary;
    }

    public SettlementSummary getSettlementSummary() {
        return settlementSummary;
    }

    public void setSettlementSummary(SettlementSummary settlementSummary) {
        this.settlementSummary = settlementSummary;
    }

    public CardDetails getCardDetails() {
        return cardDetails;
    }

    public void setCardDetails(CardDetails cardDetails) {
        this.cardDetails = cardDetails;
    }

    public SupportedLanguage getLanguage() {
        return language;
    }

    public void setLanguage(SupportedLanguage language) {
        this.language = language;
    }

    public boolean isMoto() {
        return moto;
    }

    public void setMoto(boolean moto) {
        this.moto = moto;
    }

    public Boolean getLive() {
        return live;
    }

    public void setLive(Boolean live) {
        this.live = live;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
    }

    public String getWalletType() {
        return walletType;
    }

    public void setWalletType(String walletType) {
        this.walletType = walletType;
    }

    public Map<String, Object> getExternalMetaData() {
        return externalMetaData;
    }

    public void setExternalMetaData(Map<String, Object> externalMetaData) {
        this.externalMetaData = externalMetaData;
    }

    public String getRefundedBy() {
        return refundedBy;
    }

    public void setRefundedBy(String refundedBy) {
        this.refundedBy = refundedBy;
    }

    public String getRefundedByUserEmail() {
        return refundedByUserEmail;
    }

    public void setRefundedByUserEmail(String refundedByUserEmail) {
        this.refundedByUserEmail = refundedByUserEmail;
    }

    public String getParentTransactionId() {
        return parentTransactionId;
    }

    public void setParentTransactionId(String parentTransactionId) {
        this.parentTransactionId = parentTransactionId;
    }

    public String getServiceId() { return serviceId; }

    public AuthorisationSummary getAuthorisationSummary() {
        return authorisationSummary;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setAuthorisationSummary(AuthorisationSummary authorisationSummary) {
        this.authorisationSummary = authorisationSummary;
    }
}


package uk.gov.pay.webhooks.ledger.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CardDetails {

    private String cardholderName;
    private Address billingAddress;
    private String cardBrand;
    private String lastDigitsCardNumber;
    private String firstDigitsCardNumber;
    private String expiryDate;
    private String cardType;

    public CardDetails() {

    }

    public CardDetails(String cardholderName, Address billingAddress, String cardBrand,
                       String lastDigitsCardNumber, String firstDigitsCardNumber, String cardExpiryDate, String cardType) {
        this.cardholderName = cardholderName;
        this.billingAddress = billingAddress;
        this.cardBrand = cardBrand;
        this.lastDigitsCardNumber = lastDigitsCardNumber;
        this.firstDigitsCardNumber = firstDigitsCardNumber;
        this.expiryDate = cardExpiryDate;
        this.cardType = cardType;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public Address getBillingAddress() {
        return billingAddress;
    }

    public String getCardBrand() {
        return cardBrand == null ? "" : cardBrand;
    }

    public String getLastDigitsCardNumber() {
        return lastDigitsCardNumber;
    }

    public String getFirstDigitsCardNumber() {
        return firstDigitsCardNumber;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public String getCardType() {
        return cardType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardDetails that = (CardDetails) o;
        return Objects.equals(cardholderName, that.cardholderName) &&
                Objects.equals(billingAddress, that.billingAddress) &&
                Objects.equals(cardBrand, that.cardBrand) &&
                Objects.equals(lastDigitsCardNumber, that.lastDigitsCardNumber) &&
                Objects.equals(firstDigitsCardNumber, that.firstDigitsCardNumber) &&
                Objects.equals(cardType, that.cardType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardholderName, billingAddress, cardBrand, lastDigitsCardNumber, firstDigitsCardNumber, cardType);
    }

}

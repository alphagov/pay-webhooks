package uk.gov.pay.webhooks.message.apirepresentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaymentApiCardDetails(
        String lastDigitsCardNumber,
        String firstDigitsCardNumber,
        String cardholderName,
        String expiryDate,
        PaymentApiAddress billingAddress,
        String cardBrand,
        String cardType       
) {
}

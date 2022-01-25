package uk.gov.pay.webhooks.message.apirepresentation;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PaymentApiRefundSummary(String status,
                                      long amountAvailable,
                                      long amountSubmitted) {
}

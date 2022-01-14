package uk.gov.pay.webhooks.message.apirepresentation;

public record PaymentApiRefundSummary(String status,
                                      long amountAvailable,
                                      long amountSubmitted) {
}

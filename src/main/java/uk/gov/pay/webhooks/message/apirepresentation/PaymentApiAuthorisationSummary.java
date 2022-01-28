package uk.gov.pay.webhooks.message.apirepresentation;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentApiAuthorisationSummary(@JsonProperty("three_d_secure") PaymentApiThreeDSecure paymentApiThreeDSecure) {
}

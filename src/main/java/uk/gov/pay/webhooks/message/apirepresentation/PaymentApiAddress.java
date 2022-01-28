package uk.gov.pay.webhooks.message.apirepresentation;

public record PaymentApiAddress(String line1,
                                String line2,
                                String postcode,
                                String city,
                                String country) {
}

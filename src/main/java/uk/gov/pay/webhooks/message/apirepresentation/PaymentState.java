package uk.gov.pay.webhooks.message.apirepresentation;

import com.fasterxml.jackson.annotation.JsonInclude;


@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentState(String status, boolean finished, String message, String code) {} 


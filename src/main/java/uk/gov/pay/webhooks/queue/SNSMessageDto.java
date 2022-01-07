package uk.gov.pay.webhooks.queue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SNSMessageDto(String Message,
                            String TopicArn
) {}

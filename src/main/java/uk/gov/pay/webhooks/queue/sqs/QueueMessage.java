package uk.gov.pay.webhooks.queue.sqs;

import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.List;

public record QueueMessage(String messageId, String receiptHandle, String messageBody) {

    public static List<QueueMessage> of(ReceiveMessageResponse receiveMessageResult) {
        return receiveMessageResult.messages()
                .stream()
                .map(message -> new QueueMessage(message.messageId(), message.receiptHandle(), message.body()))
                .toList();
    }

    public static QueueMessage of(SendMessageResponse messageResult, String validJsonMessage) {
        return new QueueMessage(messageResult.messageId(), null, validJsonMessage);
    }

}

package uk.gov.pay.webhooks.queue.sqs;

import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageResult;

import java.util.List;

public record QueueMessage(String messageId, String receiptHandle, String messageBody) {

    public static List<QueueMessage> of(ReceiveMessageResult receiveMessageResult) {
        return receiveMessageResult.getMessages()
                .stream()
                .map(message -> new QueueMessage(message.getMessageId(), message.getReceiptHandle(), message.getBody()))
                .toList();
    }

    public static QueueMessage of(SendMessageResult messageResult, String validJsonMessage) {
        return new QueueMessage(messageResult.getMessageId(), null, validJsonMessage);
    }

}

package uk.gov.pay.webhooks.queue.sqs;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.AmazonSQSException;
import com.amazonaws.services.sqs.model.ChangeMessageVisibilityRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.queue.QueueException;
import uk.gov.pay.webhooks.queue.QueueMessage;

import javax.inject.Inject;
import java.util.List;

public class SqsQueueService {
    private final Logger logger = LoggerFactory.getLogger(SqsQueueService.class);

    private AmazonSQS sqsClient;

    private final int messageMaximumWaitTimeInSeconds;
    private final int messageMaximumBatchSize;

    @Inject
    public SqsQueueService(AmazonSQS sqsClient, WebhooksConfig webhooksConfig) {
        this.sqsClient = sqsClient;
        this.messageMaximumBatchSize = webhooksConfig.getSqsConfig().getMessageMaximumBatchSize();
        this.messageMaximumWaitTimeInSeconds = webhooksConfig.getSqsConfig().getMessageMaximumWaitTimeInSeconds();
    }

    public List<QueueMessage> receiveMessages(String queueUrl, String messageAttributeName) throws QueueException {
        try {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(queueUrl);
            receiveMessageRequest
                    .withMessageAttributeNames(messageAttributeName)
                    .withWaitTimeSeconds(messageMaximumWaitTimeInSeconds)
                    .withMaxNumberOfMessages(messageMaximumBatchSize);

            ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);

            return QueueMessage.of(receiveMessageResult);
        } catch (Exception e) {
            logger.error("Failed to receive messages from SQS queue - [{}] {}", e.getClass().getCanonicalName(), e.getMessage());
            throw new QueueException("Failed to receive messages from SQS queue", e);
        }
    }

    public void deleteMessage(String queueUrl, String messageReceiptHandle) throws QueueException {
        try {
            sqsClient.deleteMessage(new DeleteMessageRequest(queueUrl, messageReceiptHandle));
        } catch (AmazonSQSException | UnsupportedOperationException e) {
            logger.error("Failed to delete message from SQS queue - {}", e.getMessage());
            throw new QueueException("Failed to delete message from SQS queue", e);
        } catch (AmazonServiceException e) {
            logger.error("Failed to delete message from SQS queue - [errorMessage={}] [awsErrorCode={}]", e.getMessage(), e.getErrorCode());
            throw new QueueException("Failed to delete message from SQS queue", e);
        }
    }

    public void deferMessage(String queueUrl, String messageReceiptHandle, int retryDelayInSeconds) throws QueueException {
        try {
            ChangeMessageVisibilityRequest changeMessageVisibilityRequest = new ChangeMessageVisibilityRequest(
                    queueUrl,
                    messageReceiptHandle,
                    retryDelayInSeconds
            );

            sqsClient.changeMessageVisibility(changeMessageVisibilityRequest);
        } catch (AmazonSQSException | UnsupportedOperationException e) {
            logger.error("Failed to defer message from SQS queue - {}", e.getMessage());
            throw new QueueException("Failed to defer message from SQS queue", e);
        }
    }
}

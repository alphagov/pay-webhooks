package uk.gov.pay.webhooks.queue.sqs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import uk.gov.pay.webhooks.app.WebhooksConfig;

import jakarta.inject.Inject;

import java.util.List;

public class SqsQueueService {
    private final Logger logger = LoggerFactory.getLogger(SqsQueueService.class);

    private final SqsClient sqsClient;

    private final int messageMaximumWaitTimeInSeconds;
    private final int messageMaximumBatchSize;

    @Inject
    public SqsQueueService(SqsClient sqsClient, WebhooksConfig webhooksConfig) {
        this.sqsClient = sqsClient;
        this.messageMaximumBatchSize = webhooksConfig.getSqsConfig().getMessageMaximumBatchSize();
        this.messageMaximumWaitTimeInSeconds = webhooksConfig.getSqsConfig().getMessageMaximumWaitTimeInSeconds();
    }

    public List<QueueMessage> receiveMessages(String queueUrl, String messageAttributeName) throws QueueException {
        try {
            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageAttributeNames(messageAttributeName)
                    .waitTimeSeconds(messageMaximumWaitTimeInSeconds)
                    .maxNumberOfMessages(messageMaximumBatchSize)
                    .build();

            ReceiveMessageResponse receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);

            return QueueMessage.of(receiveMessageResult);
        } catch (Exception e) {
            logger.error("Failed to receive messages from SQS queue - [{}] {}", e.getClass().getCanonicalName(), e.getMessage());
            throw new QueueException("Failed to receive messages from SQS queue", e);
        }
    }

    public void deleteMessage(String queueUrl, String messageReceiptHandle) throws QueueException {
        try {
            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(messageReceiptHandle)
                    .build();
            sqsClient.deleteMessage(deleteMessageRequest);
        } catch (SqsException | UnsupportedOperationException e) {
            logger.error("Failed to delete message from SQS queue - {}", e.getMessage());
            throw new QueueException("Failed to delete message from SQS queue", e);
        } catch (AwsServiceException e) {
            logger.error("Failed to delete message from SQS queue - [errorMessage={}] [awsErrorCode={}]", e.getMessage(), e.awsErrorDetails().errorCode());
            throw new QueueException("Failed to delete message from SQS queue", e);
        }
    }

    public void deferMessage(String queueUrl, String messageReceiptHandle, int retryDelayInSeconds) throws QueueException {
        try {
            ChangeMessageVisibilityRequest changeMessageVisibilityRequest = ChangeMessageVisibilityRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(messageReceiptHandle)
                    .visibilityTimeout(retryDelayInSeconds)
                    .build();


            sqsClient.changeMessageVisibility(changeMessageVisibilityRequest);
        } catch (SqsException | UnsupportedOperationException e) {
            logger.error("Failed to defer message from SQS queue - {}", e.getMessage());
            throw new QueueException("Failed to defer message from SQS queue", e);
        }
    }
}

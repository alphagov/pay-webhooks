package uk.gov.pay.rule;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

public class SqsTestDocker {
    private static final Logger logger = LoggerFactory.getLogger(SqsTestDocker.class);

    private static GenericContainer<?> sqsContainer;
    
    private static final Integer PORT = 9324;

    public static AmazonSQS initialise(String... queues) {
        try {
            createContainer();
            return createQueues(queues);
        } catch (Exception e) {
            logger.error("Exception initialising SQS Container - {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static void createContainer() {
        if (sqsContainer == null) {
            logger.info("Creating SQS Container");

            sqsContainer = new GenericContainer<>("softwaremill/elasticmq-native:1.4.2")
                    .withExposedPorts(PORT)
                    .waitingFor(Wait.forLogMessage(".*ElasticMQ server.*.*started.*", 1));

            sqsContainer.start();
        }
    }

    private static AmazonSQS createQueues(String... queues) {
        AmazonSQS amazonSQS = getSqsClient();
        if (queues != null) {
            for (String queue : queues) {
                amazonSQS.createQueue(queue);
            }
        }

        return amazonSQS;
    }

    public static String getQueueUrl(String queueName) {
        return getEndpoint() + "/queue/" + queueName;
    }

    public static String getEndpoint() {
        return "http://localhost:" + sqsContainer.getMappedPort(PORT);
    }

    private static AmazonSQS getSqsClient() {
        // random credentials required by AWS SDK to build SQS client
        BasicAWSCredentials awsCreds = new BasicAWSCredentials("x", "x");

        return AmazonSQSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(
                                getEndpoint(),
                                "region-1"
                        ))
                .withRequestHandlers()
                .build();
    }
}

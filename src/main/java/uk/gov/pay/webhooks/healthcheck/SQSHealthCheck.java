package uk.gov.pay.webhooks.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;
import uk.gov.pay.webhooks.app.WebhooksConfig;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class SQSHealthCheck extends HealthCheck {

    private final SqsClient sqsClient;
    private final Logger logger = LoggerFactory.getLogger(SQSHealthCheck.class);
    private List<NameValuePair> checkList = new ArrayList<>();

    @Inject
    public SQSHealthCheck(SqsClient sqsClient, WebhooksConfig webhooksConfig) {
        this.sqsClient = sqsClient;
        setUpCheckList(webhooksConfig);
    }

    @Override
    protected Result check() {
        List<String> queueChecks = checkList.stream()
                .map(this::checkQueue)
                .flatMap(Optional::stream)
                .collect(Collectors.toUnmodifiableList());
        if (!queueChecks.isEmpty()) {
            return Result.unhealthy(format("Failed queue attribute check: %s", String.join(",", queueChecks)));
        }
        
        return Result.healthy();
    }
    
    private void setUpCheckList(WebhooksConfig webhooksConfig) {
        checkList.add(new BasicNameValuePair("event", webhooksConfig.getSqsConfig().getEventQueueUrl()));
    }
    
    private Optional<String> checkQueue(NameValuePair nameValuePair) {
        GetQueueAttributesRequest queueAttributesRequest =
                GetQueueAttributesRequest.builder()
                        .queueUrl(nameValuePair.getValue())
                        .attributeNamesWithStrings("All")
                        .build();
        try {
            sqsClient.getQueueAttributes(queueAttributesRequest);
        } catch (SqsException | UnsupportedOperationException e) {
            logger.error("Failed to retrieve [{}] queue attributes - {}", nameValuePair.getName(), e.getMessage());
            return Optional.of(e.getMessage());
        } catch (SdkClientException e) {
            logger.error("Failed to connect to sqs server - {}", e.getMessage());
            return Optional.of("Failed to connect to sqs server - " + e.getMessage());
        }
        
        return Optional.empty();
    }
}

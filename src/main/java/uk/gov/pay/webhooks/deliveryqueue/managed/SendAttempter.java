package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.message.WebhookMessageSender;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SendAttempter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendAttempter.class);
    private final MetricRegistry metricRegistry;
    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private InstantSource instantSource;
    private WebhookMessageSender webhookMessageSender;

    @Inject
    public SendAttempter(WebhookDeliveryQueueDao webhookDeliveryQueueDao,
                         InstantSource instantSource,
                         WebhookMessageSender webhookMessageSender, 
                         Environment environment) {
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
        this.instantSource = instantSource;
        this.webhookMessageSender = webhookMessageSender;
        this.metricRegistry = environment.metrics();
    }

    public void attemptSend(WebhookDeliveryQueueEntity queueItem) {
        var retryCount = webhookDeliveryQueueDao.countFailed(queueItem.getWebhookMessageEntity());
        Instant start = instantSource.instant();

        try {
            LOGGER.info("Attempting to send Webhook ID %s to %s".formatted(queueItem.getWebhookMessageEntity().getExternalId(), queueItem.getWebhookMessageEntity().getWebhookEntity().getCallbackUrl()));
            var response = webhookMessageSender.sendWebhookMessage(queueItem.getWebhookMessageEntity());
            var responseTime = Duration.between(start, instantSource.instant()).toMillis();

            var statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode <= 299) {
                LOGGER.info("Message attempt succeeded with %s".formatted(statusCode));
                webhookDeliveryQueueDao.recordResult(queueItem, getReasonFromStatusCode(statusCode), responseTime, statusCode, WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL, metricRegistry);
            } else {
                LOGGER.info("Message attempt failed with %s".formatted(statusCode));
                webhookDeliveryQueueDao.recordResult(queueItem, getReasonFromStatusCode(statusCode), responseTime, statusCode, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, metricRegistry);
                enqueueRetry(queueItem, nextRetryIn(retryCount));
            }
        } catch (HttpTimeoutException e) {
            var responseTime = Duration.between(start, instantSource.instant()).toMillis();
            LOGGER.info("HTTP timeout exception %s".formatted(e.toString()));
            webhookDeliveryQueueDao.recordResult(queueItem, "HTTP Timeout after %d milliseconds".formatted(responseTime), responseTime, null, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, metricRegistry);
            enqueueRetry(queueItem, nextRetryIn(retryCount));
        } catch (IOException | InterruptedException | InvalidKeyException e) {
            var responseTime = Duration.between(start, instantSource.instant()).toMillis();
            LOGGER.warn("Exception %s attempting to send webhook message ID: %s".formatted(e.getMessage(), queueItem.getWebhookMessageEntity().getExternalId()));
            webhookDeliveryQueueDao.recordResult(queueItem, e.getMessage(), responseTime, null, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, metricRegistry);
            enqueueRetry(queueItem, nextRetryIn(retryCount));
        } catch (Exception e) {
            var responseTime = Duration.between(start, instantSource.instant()).toMillis();
            // handle all exceptions at this level to make sure that the retry mechanism is allowed to work as designed
            // allowing errors passed this point (not guaranteeing an update) would allow perpetual failures 
            LOGGER.warn("Unexpected exception %s attempting to send webhook message ID: %s".formatted(e.getMessage(), queueItem.getWebhookMessageEntity().getExternalId()));
            webhookDeliveryQueueDao.recordResult(queueItem, "Unknown error", responseTime, null, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, metricRegistry);
            enqueueRetry(queueItem, nextRetryIn(retryCount));
        }
    }

    private void enqueueRetry(WebhookDeliveryQueueEntity queueItem, Duration nextRetryIn) {
        Optional.ofNullable(nextRetryIn).ifPresent(
                retryDelay -> webhookDeliveryQueueDao.enqueueFrom(queueItem.getWebhookMessageEntity(), WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant().plus(retryDelay))
        );
    }

    private Duration nextRetryIn(Long retryCount) {
        return switch (Math.toIntExact(retryCount)) {
            case 0 -> Duration.of(60, ChronoUnit.SECONDS);
            case 1 -> Duration.of(5, ChronoUnit.MINUTES);
            case 2 -> Duration.of(1, ChronoUnit.HOURS);
            case 3 -> Duration.of(1, ChronoUnit.DAYS);
            case 4 -> Duration.of(2, ChronoUnit.DAYS);
            default -> null;
        };
    }
    
    private String getReasonFromStatusCode(int statusCode) {
        return Stream.of(String.valueOf(statusCode), Response.Status.fromStatusCode(statusCode).getReasonPhrase())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }
}

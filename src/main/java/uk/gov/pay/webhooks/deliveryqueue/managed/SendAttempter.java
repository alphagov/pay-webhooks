package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.setup.Environment;
import net.logstash.logback.marker.Markers;
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
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.gov.pay.webhooks.app.WebhooksKeys.ERROR_MESSAGE;
import static uk.gov.pay.webhooks.app.WebhooksKeys.STATE_TRANSITION_TO_STATE;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_CALLBACK_URL;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_RETRY_COUNT;
import static uk.gov.service.payments.logging.LoggingKeys.HTTP_STATUS;

public class SendAttempter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendAttempter.class);
    private final MetricRegistry metricRegistry;
    private final WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private final InstantSource instantSource;
    private final WebhookMessageSender webhookMessageSender;

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

        try {
            LOGGER.info(
                    Markers.append(WEBHOOK_CALLBACK_URL, queueItem.getWebhookMessageEntity().getWebhookEntity().getCallbackUrl())
                            .and(Markers.append(WEBHOOK_MESSAGE_RETRY_COUNT, retryCount)),
                    "Sending webhook message"
            );
            var response = webhookMessageSender.sendWebhookMessage(queueItem.getWebhookMessageEntity());

            var statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode <= 299) {
                // @TODO(sfount): add response_time in PR to record and persist that information
                LOGGER.info(
                        Markers.append(HTTP_STATUS, statusCode)
                                .and(Markers.append(WEBHOOK_MESSAGE_RETRY_COUNT, retryCount))
                                .and(Markers.append(STATE_TRANSITION_TO_STATE, WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL)),
                        "Webhook message sent"
                );
                webhookDeliveryQueueDao.recordResult(queueItem, getReasonFromStatusCode(statusCode), statusCode, WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL, metricRegistry);
            } else {
                LOGGER.info(
                        Markers.append(HTTP_STATUS, statusCode)
                                .and(Markers.append(STATE_TRANSITION_TO_STATE, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED)),
                        "Webhook message failed to send"
                );
                webhookDeliveryQueueDao.recordResult(queueItem, getReasonFromStatusCode(statusCode), statusCode, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, metricRegistry);
                enqueueRetry(queueItem, nextRetryIn(retryCount));
            }
        } catch (HttpTimeoutException e) {
            LOGGER.info(
                    Markers.append(ERROR_MESSAGE, e.getMessage())
                            .and(Markers.append(STATE_TRANSITION_TO_STATE, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED)),
                    "Webhook message timed out"
            );
            webhookDeliveryQueueDao.recordResult(queueItem, "HTTP Timeout", null, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, metricRegistry);
            enqueueRetry(queueItem, nextRetryIn(retryCount));
        } catch (IOException | InterruptedException | InvalidKeyException e) {
            LOGGER.warn(
                    Markers.append(ERROR_MESSAGE, e.getMessage())
                    .and(Markers.append(STATE_TRANSITION_TO_STATE, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED)),
                    "Webhook message failed with exception"
            );
            webhookDeliveryQueueDao.recordResult(queueItem, e.getMessage(), null, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, metricRegistry);
            enqueueRetry(queueItem, nextRetryIn(retryCount));
        } catch (Exception e) {
            // handle all exceptions at this level to make sure that the retry mechanism is allowed to work as designed
            // allowing errors passed this point (not guaranteeing an update) would allow perpetual failures 
            LOGGER.warn(
                    Markers.append(ERROR_MESSAGE, e.getMessage())
                            .and(Markers.append(STATE_TRANSITION_TO_STATE, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED)),
                    "Webhook message failed for unknown reason"
            );
            webhookDeliveryQueueDao.recordResult(queueItem, "Unknown error", null, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, metricRegistry);
            enqueueRetry(queueItem, nextRetryIn(retryCount));
        }
    }

    private void enqueueRetry(WebhookDeliveryQueueEntity queueItem, Duration nextRetryIn) {
        Optional.ofNullable(nextRetryIn).ifPresent(retryDelay -> {
            var retryDate = instantSource.instant().plus(retryDelay);
            LOGGER.info(
                    Markers.append(STATE_TRANSITION_TO_STATE, WebhookDeliveryQueueEntity.DeliveryStatus.PENDING),
                    "Scheduling webhook message retry"
            );
            webhookDeliveryQueueDao.enqueueFrom(queueItem.getWebhookMessageEntity(), WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, retryDate);
        });
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

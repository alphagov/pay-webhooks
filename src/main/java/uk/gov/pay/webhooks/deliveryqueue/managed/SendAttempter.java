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
import java.net.URI;
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

import static uk.gov.pay.webhooks.app.WebhooksKeys.ERROR_MESSAGE;
import static uk.gov.pay.webhooks.app.WebhooksKeys.STATE_TRANSITION_TO_STATE;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_CALLBACK_URL;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_CALLBACK_URL_DOMAIN;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_ATTEMPT_RESPONSE_REASON;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_RETRY_COUNT;
import static uk.gov.pay.webhooks.app.WebhooksKeys.WEBHOOK_MESSAGE_TIME_TO_EMIT_IN_MILLIS;
import static uk.gov.service.payments.logging.LoggingKeys.HTTP_STATUS;
import static uk.gov.service.payments.logging.LoggingKeys.RESPONSE_TIME;

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
        Instant start = instantSource.instant();

        try {
            var url = queueItem.getWebhookMessageEntity().getWebhookEntity().getCallbackUrl();
            var uri = new URI(url);
            
            LOGGER.info(
                    Markers.append(WEBHOOK_CALLBACK_URL, queueItem.getWebhookMessageEntity().getWebhookEntity().getCallbackUrl())
                            .and(Markers.append(WEBHOOK_MESSAGE_RETRY_COUNT, retryCount))
                            .and(Markers.append(WEBHOOK_CALLBACK_URL_DOMAIN, uri.getHost()))
                            .and(Markers.append(WEBHOOK_MESSAGE_TIME_TO_EMIT_IN_MILLIS, Duration.between(queueItem.getCreatedDate(), instantSource.instant()).toMillis())),
                    "Sending webhook message"
            ); 
            var response = webhookMessageSender.sendWebhookMessage(queueItem.getWebhookMessageEntity());

            var statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode <= 299) {
                handleResponse(queueItem, WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL, statusCode, getReasonFromStatusCode(statusCode), retryCount, start);
            } else {
                handleResponse(queueItem, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, statusCode, getReasonFromStatusCode(statusCode), retryCount, start);
            }
        } catch (HttpTimeoutException e) {
            LOGGER.info("Request timed out");
            handleResponse(queueItem, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, null, "HTTP Timeout", retryCount, start);
        } catch (IOException | InterruptedException | InvalidKeyException e) {
            LOGGER.info(
                    Markers.append(ERROR_MESSAGE, e.getMessage()),
                    "Exception caught by request"
            );
            handleResponse(queueItem, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, null, e.getMessage(), retryCount, start);
        } catch (Exception e) {
            var responseTime = Duration.between(start, instantSource.instant());
            // handle all exceptions at this level to make sure that the retry mechanism is allowed to work as designed
            // allowing errors passed this point (not guaranteeing an update) would allow perpetual failures 
            LOGGER.warn(
                    Markers.append(ERROR_MESSAGE, e.getMessage()),
                    "Unexpected exception thrown by request"
            );
            handleResponse(queueItem, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED, null, "Unknown error", retryCount, start);
        }
    }
    
    private void handleResponse(WebhookDeliveryQueueEntity webhookDeliveryQueueEntity, WebhookDeliveryQueueEntity.DeliveryStatus status, Integer statusCode, String reason, Long retryCount, Instant startTime) {
        var responseTime = Duration.between(startTime, instantSource.instant());
        LOGGER.info(
                Markers.append(HTTP_STATUS, statusCode)
                        .and(Markers.append(WEBHOOK_MESSAGE_RETRY_COUNT, retryCount))
                        .and(Markers.append(STATE_TRANSITION_TO_STATE, status))
                        .and(Markers.append(RESPONSE_TIME, responseTime.toMillis()))
                        .and(Markers.append(WEBHOOK_MESSAGE_ATTEMPT_RESPONSE_REASON, reason)),
                "Sending webhook message finished"
        ); 
        webhookDeliveryQueueDao.recordResult(webhookDeliveryQueueEntity, reason, responseTime, statusCode, status, metricRegistry);
        
        if (!status.equals(WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL)) {
            enqueueRetry(webhookDeliveryQueueEntity, nextRetryIn(retryCount));
        }
    }

    private void enqueueRetry(WebhookDeliveryQueueEntity queueItem, Duration nextRetryIn) {
        Optional.ofNullable(nextRetryIn).ifPresentOrElse(retryDelay -> {
            LOGGER.info("Scheduling webhook message for retry");
            webhookDeliveryQueueDao.enqueueFrom(queueItem.getWebhookMessageEntity(), WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, instantSource.instant().plus(retryDelay));
        }, () -> {
            LOGGER.warn("Webhook message terminally failed to deliver");
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

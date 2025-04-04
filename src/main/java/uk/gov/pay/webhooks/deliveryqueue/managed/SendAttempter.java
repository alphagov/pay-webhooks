package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.core.setup.Environment;
import net.logstash.logback.marker.LogstashMarker;
import net.logstash.logback.marker.Markers;
import org.apache.http.NoHttpResponseException;
import org.apache.http.conn.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.deliveryqueue.DeliveryStatus;
import uk.gov.pay.webhooks.deliveryqueue.WebhookNotActiveException;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.message.WebhookMessageSender;
import uk.gov.pay.webhooks.validations.CallbackUrlDomainNotOnAllowListException;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.http.HttpTimeoutException;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

    private final List<DeliveryStatus> terminalStatuses = List.of(
            DeliveryStatus.SUCCESSFUL,
            DeliveryStatus.WILL_NOT_SEND
    );

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
        var webhook = queueItem.getWebhookMessageEntity().getWebhookEntity();
        var retryCount = webhookDeliveryQueueDao.countFailed(queueItem.getWebhookMessageEntity());
        Instant start = instantSource.instant();

        URL url = null;
        
        try {
            url = new URL(webhook.getCallbackUrl().strip());
        } catch (MalformedURLException e) {
            handleGenericException(queueItem, retryCount, start, e);
        }

        try {

            LOGGER.info(
                    Markers.append(WEBHOOK_CALLBACK_URL, queueItem.getWebhookMessageEntity().getWebhookEntity().getCallbackUrl())
                            .and(Markers.append(WEBHOOK_MESSAGE_RETRY_COUNT, retryCount))
                            .and(Markers.append(WEBHOOK_CALLBACK_URL_DOMAIN, url.getHost()))
                            .and(Markers.append(WEBHOOK_MESSAGE_TIME_TO_EMIT_IN_MILLIS, Duration.between(queueItem.getSendAt(), instantSource.instant()).toMillis())),
                    "Sending webhook message started"
            ); 
            var response = webhookMessageSender.sendWebhookMessage(queueItem.getWebhookMessageEntity());

            var statusCode = response.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode <= 299) {
                handleResponse(queueItem, DeliveryStatus.SUCCESSFUL, statusCode, getReasonFromStatusCode(statusCode), retryCount, start, Optional.of(url));
            } else {
                handleResponse(queueItem, DeliveryStatus.FAILED, statusCode, getReasonFromStatusCode(statusCode), retryCount, start, Optional.of(url));
            }
        } catch (SocketTimeoutException | HttpTimeoutException | NoHttpResponseException | ConnectTimeoutException e) {
            LOGGER.info("Request timed out");
            handleResponse(queueItem, DeliveryStatus.FAILED, null, "HTTP Timeout", retryCount, start, Optional.of(url));
        } catch (IOException | InterruptedException | InvalidKeyException e) {
            LOGGER.info(
                    Markers.append(ERROR_MESSAGE, e.getMessage()),
                    "Exception caught by request"
            );
            handleResponse(queueItem, DeliveryStatus.FAILED, null, e.getMessage(), retryCount, start);
        } catch (WebhookNotActiveException e) {
            LOGGER.info("Not sending webhook message for non-active webhook");
            handleResponse(queueItem, DeliveryStatus.WILL_NOT_SEND, null, "Webhook not active", retryCount, start);
        } catch (CallbackUrlDomainNotOnAllowListException e) {
            LOGGER.error(
                    Markers.append(WEBHOOK_CALLBACK_URL_DOMAIN, e.getUrl()),
                    "Attempt to send to a domain not on the allow list has been blocked"
            );
            handleResponse(queueItem, DeliveryStatus.WILL_NOT_SEND, null, "Violates security rules", retryCount, start);
        } catch (Exception e) {
            handleGenericException(queueItem, retryCount, start, e);
        }
    }

    private void handleGenericException(WebhookDeliveryQueueEntity queueItem, Long retryCount, Instant start, Exception e) {
        // handle all exceptions at this level to make sure that the retry mechanism is allowed to work as designed
        // allowing errors passed this point (not guaranteeing an update) would allow perpetual failures 
        LOGGER.warn(
                Markers.append(ERROR_MESSAGE, e.getMessage()),
                "Unexpected exception thrown by request"
        );
        handleResponse(queueItem, DeliveryStatus.FAILED, null, "Unknown error", retryCount, start);
    }

    private void handleResponse(WebhookDeliveryQueueEntity webhookDeliveryQueueEntity,
                                DeliveryStatus status,
                                Integer statusCode,
                                String reason,
                                Long retryCount,
                                Instant startTime) {
        handleResponse(webhookDeliveryQueueEntity, status, statusCode, reason, retryCount, startTime, Optional.empty());
    }

    private void handleResponse(WebhookDeliveryQueueEntity webhookDeliveryQueueEntity, 
                                DeliveryStatus status, 
                                Integer statusCode, 
                                String reason, 
                                Long retryCount, 
                                Instant startTime,
                                Optional<URL> domain) {
        var responseTime = Duration.between(startTime, instantSource.instant());
        
        LogstashMarker logstashMarker = Markers.append(HTTP_STATUS, statusCode)
                .and(Markers.append(WEBHOOK_MESSAGE_RETRY_COUNT, retryCount))
                .and(Markers.append(STATE_TRANSITION_TO_STATE, status))
                .and(Markers.append(RESPONSE_TIME, responseTime.toMillis()))
                .and(Markers.append(WEBHOOK_MESSAGE_ATTEMPT_RESPONSE_REASON, reason));
        
        domain.ifPresent(d -> logstashMarker.and(Markers.append(WEBHOOK_CALLBACK_URL_DOMAIN, d.getHost())));
        
        LOGGER.info(logstashMarker, "Sending webhook message finished"); 
        
        webhookDeliveryQueueDao.recordResult(webhookDeliveryQueueEntity, reason, responseTime, statusCode, status, metricRegistry);
        webhookDeliveryQueueEntity.getWebhookMessageEntity().setLastDeliveryStatus(status);

        if (!terminalStatuses.contains(status)) {
            enqueueRetry(webhookDeliveryQueueEntity, nextRetryIn(retryCount));
        }
    }

    private void enqueueRetry(WebhookDeliveryQueueEntity queueItem, Duration nextRetryIn) {
        Optional.ofNullable(nextRetryIn).ifPresentOrElse(retryDelay -> {
            LOGGER.info("Scheduling webhook message for retry");
            webhookDeliveryQueueDao.enqueueFrom(queueItem.getWebhookMessageEntity(), DeliveryStatus.PENDING, instantSource.instant().plus(retryDelay));
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

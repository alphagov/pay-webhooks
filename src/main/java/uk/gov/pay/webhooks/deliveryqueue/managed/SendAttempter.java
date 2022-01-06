package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.message.WebhookMessageSender;
import uk.gov.pay.webhooks.message.WebhookMessageSignatureGenerator;

import javax.inject.Inject;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.security.InvalidKeyException;
import java.time.Duration;
import java.time.InstantSource;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

public class SendAttempter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendAttempter.class);
    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private InstantSource instantSource;
    private WebhookMessageSender webhookMessageSender;

    @Inject
    public SendAttempter(WebhookDeliveryQueueDao webhookDeliveryQueueDao, InstantSource instantSource, WebhookMessageSender webhookMessageSender) {
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
        this.instantSource = instantSource;
        this.webhookMessageSender = webhookMessageSender;
    }

    public void attemptSend(WebhookDeliveryQueueEntity queueItem) {
        var retryCount = webhookDeliveryQueueDao.countFailed(queueItem.getWebhookMessageEntity());

        try {
            LOGGER.info("Attempting to send Webhook ID %s to %s".formatted(queueItem.getWebhookMessageEntity().getExternalId(), queueItem.getWebhookMessageEntity().getWebhookEntity().getCallbackUrl()));
            var response = webhookMessageSender.sendWebhookMessage(queueItem.getWebhookMessageEntity());
            var statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode <= 299) {
                webhookDeliveryQueueDao.recordResult(queueItem, String.valueOf(statusCode), statusCode, WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL);
            } else {
                webhookDeliveryQueueDao.recordResult(queueItem, String.valueOf(statusCode), statusCode, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED);
                enqueueRetry(queueItem, nextRetryIn(retryCount));
            }
        } catch (HttpTimeoutException e) {
            webhookDeliveryQueueDao.recordResult(queueItem, "HTTP Timeout after 5 seconds", null, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED);
            enqueueRetry(queueItem, nextRetryIn(retryCount));
        } catch (IOException | InterruptedException | InvalidKeyException e) {
            LOGGER.warn("Unexpected exception %s attempting to send webhook message ID: %s".formatted(e.getMessage(), queueItem.getWebhookMessageEntity().getExternalId()));
            webhookDeliveryQueueDao.recordResult(queueItem, e.getMessage(), null, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED);
            enqueueRetry(queueItem, nextRetryIn(retryCount));

        }
    }

    private void enqueueRetry(WebhookDeliveryQueueEntity queueItem, Duration nextRetryIn) {
        Optional.ofNullable(nextRetryIn).ifPresent(
                retryDelay -> webhookDeliveryQueueDao.enqueueFrom(queueItem.getWebhookMessageEntity(), WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, Date.from(instantSource.instant().plus(retryDelay)))
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
}

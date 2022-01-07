package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebhookMessageSendingQueueProcessor implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookMessageSendingQueueProcessor.class);

    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private final InstantSource instantSource;
    private final SessionFactory sessionFactory;
    private final ScheduledExecutorService scheduledExecutorService;
    private final ObjectMapper objectMapper;
    private final WebhookMessageSignatureGenerator webhookMessageSignatureGenerator;
    private final HttpClient httpClient;
    
    @Inject
    public WebhookMessageSendingQueueProcessor(Environment environment, WebhookDeliveryQueueDao webhookDeliveryQueueDao, ObjectMapper objectMapper, InstantSource instantSource, SessionFactory sessionFactory, WebhookMessageSignatureGenerator webhookMessageSignatureGenerator, HttpClient httpClient) {
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
        this.instantSource = instantSource;
        this.sessionFactory = sessionFactory;
        this.objectMapper = objectMapper;
        this.webhookMessageSignatureGenerator = webhookMessageSignatureGenerator;

        scheduledExecutorService = environment
                .lifecycle()
                .scheduledExecutorService("retries")
                .threads(1)
                .build();
        this.httpClient = httpClient;
    }

    @Override
    public void start() {
        scheduledExecutorService.scheduleWithFixedDelay(
                this::processQueue,
                30,
                1,
                TimeUnit.SECONDS
        );
    }

    public void processQueue() {
        try {
            pollQueue();
        } catch (Exception e) {
            LOGGER.warn("Failed to poll queue %s".formatted(e.getMessage()));
            e.printStackTrace();
        }
    }

    private void attemptSend(WebhookDeliveryQueueEntity queueItem) {
        var retryCount = webhookDeliveryQueueDao.countFailed(queueItem.getWebhookMessageEntity());

        var webhookMessageSender = new WebhookMessageSender(httpClient, objectMapper, webhookMessageSignatureGenerator);
        try {
            LOGGER.info("Attempting to send Webhook ID %s to %s".formatted(queueItem.getWebhookMessageEntity().getExternalId(), queueItem.getWebhookMessageEntity().getWebhookEntity().getCallbackUrl()));
            var response = webhookMessageSender.sendWebhookMessage(queueItem.getWebhookMessageEntity());

            var statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode <= 299) {
                LOGGER.info("Message attempt succeeded with %s".formatted(statusCode));
                webhookDeliveryQueueDao.recordResult(queueItem, String.valueOf(statusCode), statusCode, WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL);
            } else {
                LOGGER.info("Message attempt failed with %s".formatted(statusCode));
                webhookDeliveryQueueDao.recordResult(queueItem, String.valueOf(statusCode), statusCode, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED);
                enqueueRetry(queueItem, nextRetryIn(retryCount));
            }
        } catch (HttpTimeoutException e) {
            LOGGER.info("HTTP timeout exception %s".formatted(e.toString()));
            webhookDeliveryQueueDao.recordResult(queueItem, "HTTP Timeout after 5 seconds", null, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED);
            enqueueRetry(queueItem, nextRetryIn(retryCount));
        } catch (IOException | InterruptedException | InvalidKeyException e) {
            LOGGER.warn("Exception %s attempting to send webhook message ID: %s".formatted(e.getMessage(), queueItem.getWebhookMessageEntity().getExternalId()));
            webhookDeliveryQueueDao.recordResult(queueItem, e.getMessage(), null, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED);
            enqueueRetry(queueItem, nextRetryIn(retryCount));
        } catch (Exception e) {
            // handle all exceptions at this level to make sure that the retry mechanism is allowed to work as designed
            // allowing errors passed this point (not guaranteeing an update) would allow perpetual failures 
            LOGGER.warn("Unexpected exception %s attempting to send webhook message ID: %s".formatted(e.getMessage(), queueItem.getWebhookMessageEntity().getExternalId()));
            webhookDeliveryQueueDao.recordResult(queueItem, "Unknown error", null, WebhookDeliveryQueueEntity.DeliveryStatus.FAILED);
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

    @Override
    public void stop() {
        scheduledExecutorService.shutdown();
    }

    private void pollQueue() {
        Session session = sessionFactory.openSession();
        ManagedSessionContext.bind(session);
        Transaction transaction = session.beginTransaction();
        try {
            Optional<WebhookDeliveryQueueEntity> maybeQueueItem = webhookDeliveryQueueDao.nextToSend(Date.from(instantSource.instant()));
            maybeQueueItem.ifPresent(this::attemptSend);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.warn("Unexpected exception when polling queue  %s: ".formatted(e.getMessage()));
            transaction.rollback();
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
        }
    }
}

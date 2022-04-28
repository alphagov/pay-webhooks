package uk.gov.pay.webhooks.deliveryqueue.managed;

import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.app.WebhookMessageSendingQueueProcessorConfig;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.message.WebhookMessageSender;

import javax.inject.Inject;
import java.time.InstantSource;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebhookMessageSendingQueueProcessor implements Managed {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookMessageSendingQueueProcessor.class);

    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private final InstantSource instantSource;
    private final SessionFactory sessionFactory;
    private final ScheduledExecutorService scheduledExecutorService;
    private final SendAttempter sendAttempter;
    private final WebhookMessageSendingQueueProcessorConfig config;
    
    @Inject
    public WebhookMessageSendingQueueProcessor(Environment environment,
                                               WebhookDeliveryQueueDao webhookDeliveryQueueDao,
                                               InstantSource instantSource,
                                               SessionFactory sessionFactory,
                                               WebhookMessageSender webhookMessageSender,
                                               WebhooksConfig configuration) {
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
        this.instantSource = instantSource;
        this.sessionFactory = sessionFactory;
        this.config = configuration.getWebhookMessageSendingQueueProcessorConfig();

        scheduledExecutorService = environment
                .lifecycle()
                .scheduledExecutorService("retries")
                .threads(config.getNumberOfThreads())
                .build();

        sendAttempter = new SendAttempter(webhookDeliveryQueueDao, instantSource, webhookMessageSender, environment);
    }

    @Override
    public void start() {
        scheduledExecutorService.scheduleWithFixedDelay(
                this::processQueue,
                config.getInitialDelayInMilliseconds(),
                config.getThreadDelayInMilliseconds(), 
                TimeUnit.MILLISECONDS
        );
    }

    public void processQueue() {
        try {
            Session session = sessionFactory.openSession();
            ManagedSessionContext.bind(session);
            Optional<WebhookDeliveryQueueEntity> nextToSend;
            do {
                 nextToSend = attemptSendIfAvailable(session);
            } while (nextToSend.isPresent());
        } catch (Exception e) {
            LOGGER.warn("Failed to poll queue %s".formatted(e.getMessage()));
            e.printStackTrace();
        } 
    }

    private Optional<WebhookDeliveryQueueEntity> attemptSendIfAvailable(Session session) {
        Transaction transaction = session.beginTransaction();
        try {
            webhookDeliveryQueueDao.nextToSend(instantSource.instant()).ifPresent(queueItem -> {
                sendAttempter.attemptSend(queueItem);
                transaction.commit();
            });
        } catch (Exception e) {
            LOGGER.error("Unexpected exception when attempting to send", e);
            transaction.rollback();
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
        }
        return webhookDeliveryQueueDao.nextToSend(instantSource.instant());
    }

    @Override
    public void stop() {
        scheduledExecutorService.shutdown();
    }
    
}

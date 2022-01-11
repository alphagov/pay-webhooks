package uk.gov.pay.webhooks.deliveryqueue.managed;

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

import javax.inject.Inject;
import java.time.InstantSource;
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
    private final SendAttempter sendAttempter;
    
    @Inject
    public WebhookMessageSendingQueueProcessor(Environment environment, WebhookDeliveryQueueDao webhookDeliveryQueueDao, InstantSource instantSource, SessionFactory sessionFactory, WebhookMessageSender webhookMessageSender) {
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
        this.instantSource = instantSource;
        this.sessionFactory = sessionFactory;

        scheduledExecutorService = environment
                .lifecycle()
                .scheduledExecutorService("retries")
                .threads(1)
                .build();

        sendAttempter = new SendAttempter(webhookDeliveryQueueDao, instantSource, webhookMessageSender);
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
            maybeQueueItem.ifPresent(sendAttempter::attemptSend);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.warn("Unexpected exception when polling queue  %s: ".formatted(e.getMessage()));
            transaction.rollback();
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
        }
    }
}

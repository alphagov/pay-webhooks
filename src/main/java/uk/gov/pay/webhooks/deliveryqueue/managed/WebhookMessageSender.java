package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;

import javax.inject.Inject;
import java.time.InstantSource;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebhookMessageSender implements Managed {

    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private final ObjectMapper objectMapper;
    private final InstantSource instantSource;
    private final SessionFactory sessionFactory;
    private final ScheduledExecutorService scheduledExecutorService;

    @Inject
    public WebhookMessageSender(Environment environment, WebhookDeliveryQueueDao webhookDeliveryQueueDao, ObjectMapper objectMapper, InstantSource instantSource, SessionFactory sessionFactory) {
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
        this.objectMapper = objectMapper;
        this.instantSource = instantSource;
        this.sessionFactory = sessionFactory;

        scheduledExecutorService = environment
                .lifecycle()
                .scheduledExecutorService("retries")
                .threads(1)
                .build();
    }

    @Override
    public void start() {
        scheduledExecutorService.scheduleWithFixedDelay(
                this::processQueue,
                30,
                30,
                TimeUnit.SECONDS
        );
    }
    public void processQueue() {
        try {
            Session session = sessionFactory.openSession();
            Optional<WebhookDeliveryQueueEntity> maybeQueueItem = pollQueue(session);
            maybeQueueItem.ifPresent(this::attemptSend);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void attemptSend(WebhookDeliveryQueueEntity queueItem) {
//    Do HTTP sending
      webhookDeliveryQueueDao.recordResult(queueItem, "200 OK", 200, WebhookDeliveryQueueEntity.DeliveryStatus.SUCCESSFUL);
//    Retry after failure
      webhookDeliveryQueueDao.enqueueFrom(queueItem.getWebhookMessageEntity(), WebhookDeliveryQueueEntity.DeliveryStatus.PENDING, Date.from(instantSource.instant().plusSeconds(60)));
    }

    @Override
    public void stop() throws Exception {

    }

    private Optional<WebhookDeliveryQueueEntity> pollQueue(Session session) {
        try (session) {
            ManagedSessionContext.bind(session);
            Transaction transaction = session.beginTransaction();
            try {
                return webhookDeliveryQueueDao.nextToSend(Date.from(instantSource.instant()));
            } catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            } finally {
                session.close();
                ManagedSessionContext.unbind(sessionFactory);
            }

        }
    }
}

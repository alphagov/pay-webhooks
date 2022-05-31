package uk.gov.pay.webhooks.deliveryqueue.managed;

import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;

import javax.inject.Inject;
import java.util.Optional;

public class WebhookMessagePollingService {
    private final WebhookDeliveryQueueDao webhookDeliveryQueueDao;

    private final SendAttempter sendAttempter;

    private final SessionFactory sessionFactory;

    @Inject
    public WebhookMessagePollingService(WebhookDeliveryQueueDao webhookDeliveryQueueDao, SendAttempter sendAttempter, SessionFactory sessionFactory) {
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
        this.sendAttempter = sendAttempter;
        this.sessionFactory = sessionFactory;
    }

    public void pollWebhookMessageQueue() {
        Optional<WebhookDeliveryQueueEntity> attemptCursor;

        do {
            attemptCursor = sendIfAvailable();
        } while(attemptCursor.isPresent());
    }

    private Optional<WebhookDeliveryQueueEntity> sendIfAvailable() {
        var session = sessionFactory.openSession();

        try {
            ManagedSessionContext.bind(session);
            var transaction = session.beginTransaction();
            try {
                return webhookDeliveryQueueDao.nextToSend()
                        .map(webhookDeliveryQueueEntity -> {
                            sendAttempter.attemptSend(webhookDeliveryQueueEntity);
                            transaction.commit();
                            return webhookDeliveryQueueEntity;
                        });
            } catch (Exception e) {
                transaction.rollback();
                return Optional.empty();
            }
        } finally {
            session.close();
            ManagedSessionContext.unbind(sessionFactory);
        }
    }
}

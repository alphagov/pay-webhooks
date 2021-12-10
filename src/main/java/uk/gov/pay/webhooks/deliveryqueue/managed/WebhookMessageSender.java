package uk.gov.pay.webhooks.deliveryqueue.managed;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.lifecycle.Managed;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;

import javax.inject.Inject;
import java.time.InstantSource;
import java.util.Date;

public class WebhookMessageSender implements Managed {

    private WebhookDeliveryQueueDao webhookDeliveryQueueDao;
    private final ObjectMapper objectMapper;
    private final InstantSource instantSource;
    private final SessionFactory sessionFactory;

    @Inject
    public WebhookMessageSender(WebhookDeliveryQueueDao webhookDeliveryQueueDao, ObjectMapper objectMapper, InstantSource instantSource, SessionFactory sessionFactory) {
        this.webhookDeliveryQueueDao = webhookDeliveryQueueDao;
        this.objectMapper = objectMapper;
        this.instantSource = instantSource;
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void start() throws Exception {
        Session session = sessionFactory.openSession();
        try (session) {
            ManagedSessionContext.bind(session);
            Transaction transaction = session.beginTransaction();
            try {
                webhookDeliveryQueueDao.nextToSend(Date.from(instantSource.instant()));
            } catch (Exception e) {
                transaction.rollback();
                throw new RuntimeException(e);
            } finally {
                session.close();
                ManagedSessionContext.unbind(sessionFactory);
            }
        }
    }

    @Override
    public void stop() throws Exception {

    }
}

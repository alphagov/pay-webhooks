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
import java.net.http.HttpClient;
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
            Session session = sessionFactory.openSession();
            pollQueue(session);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        scheduledExecutorService.shutdown();
    }

    private void pollQueue(Session session) {
            ManagedSessionContext.bind(session);
            Transaction transaction = session.beginTransaction();
            try (session) {
                Optional<WebhookDeliveryQueueEntity> maybeQueueItem = webhookDeliveryQueueDao.nextToSend(Date.from(instantSource.instant()));
                maybeQueueItem.ifPresent(item -> new SendAttempter(webhookDeliveryQueueDao, instantSource, new WebhookMessageSender(httpClient, objectMapper, webhookMessageSignatureGenerator)).attemptSend(item));
                transaction.commit();
            } catch (Exception e) {
                LOGGER.warn("Unexpected exception when polling queue  %s: ".formatted(e.getMessage()));
                transaction.rollback();
            } finally {
                ManagedSessionContext.unbind(sessionFactory);
            }
            
    }
}

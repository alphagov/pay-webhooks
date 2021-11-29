package uk.gov.pay.webhooks.queue.managed;

import com.google.inject.Inject;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.queue.EventMessageHandler;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QueueMessageReceiver implements Managed {

    private static final String QUEUE_MESSAGE_RECEIVER_THREAD_NAME = "queue-message-receiver-%d";
    private static final Logger LOGGER = LoggerFactory.getLogger(QueueMessageReceiver.class);

    private final int queueReadScheduleNumberOfThreads;

    private ScheduledExecutorService scheduledExecutorService;
    private EventMessageHandler eventMessageHandler;
    private SessionFactory sessionFactory;

    @Inject
    public QueueMessageReceiver(
            Environment environment,
            WebhooksConfig configuration,
            EventMessageHandler eventMessageHandler,
            SessionFactory sessionFactory) {
        this.eventMessageHandler = eventMessageHandler;
        this.queueReadScheduleNumberOfThreads = 1;
        this.sessionFactory = sessionFactory;

        scheduledExecutorService = environment
                .lifecycle()
                .scheduledExecutorService(QUEUE_MESSAGE_RECEIVER_THREAD_NAME)
                .threads(queueReadScheduleNumberOfThreads)
                .build();
    }

    @Override
    public void start() {
        long initialDelay = 2;
        long delay = 2;

        for(int i = 0; i < queueReadScheduleNumberOfThreads; i++) {
            scheduledExecutorService.scheduleWithFixedDelay(
                    this::receive,
                    initialDelay,
                    delay,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private void receive() {
        LOGGER.info("Queue message receiver thread polling queue");
        Session session = sessionFactory.openSession();
        try (session) {
            ManagedSessionContext.bind(session);
            eventMessageHandler.handle();
        } catch (Exception e) {
            LOGGER.error("Queue message receiver thread exception", e);
        } finally {
            ManagedSessionContext.unbind(sessionFactory);
        }
    }

    @Override
    public void stop() {
        scheduledExecutorService.shutdown();
    }
}

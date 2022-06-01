package uk.gov.pay.webhooks.queue;

import com.google.inject.Inject;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.webhooks.app.QueueMessageReceiverConfig;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.message.WebhookMessageService;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QueueMessageReceiver implements Managed {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueMessageReceiver.class);

    private static final String QUEUE_MESSAGE_RECEIVER_THREAD_NAME = "queue-message-receiver-%d";

    private final QueueMessageReceiverConfig config;
    private final ScheduledExecutorService scheduledExecutorService;
    private final EventMessageHandler eventMessageHandler;
    private final int queueReadScheduleNumberOfThreads;

    @Inject
    public QueueMessageReceiver(
            Environment environment,
            WebhooksConfig configuration,
            UnitOfWorkAwareProxyFactory unitOfWorkAwareProxyFactory,
            EventQueue eventQueue,
            WebhookMessageService webhookMessageService) {
        this.config = configuration.getQueueMessageReceiverConfig();
        this.queueReadScheduleNumberOfThreads = config.getNumberOfThreads();
        this.eventMessageHandler = unitOfWorkAwareProxyFactory.create(
                EventMessageHandler.class,
                new Class[] { EventQueue.class, WebhookMessageService.class },
                new Object[] { eventQueue, webhookMessageService }
        );

        scheduledExecutorService = environment
                .lifecycle()
                .scheduledExecutorService(QUEUE_MESSAGE_RECEIVER_THREAD_NAME)
                .threads(queueReadScheduleNumberOfThreads)
                .build();
    }

    @Override
    public void start() {
        long initialDelay = config.getThreadDelayInMilliseconds();
        long delay = config.getThreadDelayInMilliseconds();

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
        try {
            eventMessageHandler.handle();
        } catch (Exception e) {
            LOGGER.error("Queue message receiver thread exception", e);
        }
    }

    @Override
    public void stop() {
        scheduledExecutorService.shutdown();
    }

}

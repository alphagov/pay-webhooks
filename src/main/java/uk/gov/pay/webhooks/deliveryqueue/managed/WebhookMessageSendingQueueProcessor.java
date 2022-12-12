package uk.gov.pay.webhooks.deliveryqueue.managed;

import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import uk.gov.pay.webhooks.app.WebhookMessageSendingQueueProcessorConfig;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.message.WebhookMessageSender;

import javax.inject.Inject;
import java.time.InstantSource;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static uk.gov.pay.webhooks.app.WebhooksKeys.JOB_BATCH_ID;

public class WebhookMessageSendingQueueProcessor implements Managed {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookMessageSendingQueueProcessor.class);
    private final ScheduledExecutorService scheduledExecutorService;
    private final WebhookMessageSendingQueueProcessorConfig config;

    private final WebhookMessagePollingService webhookMessagePollingService;
    
    @Inject
    public WebhookMessageSendingQueueProcessor(
            Environment environment,
            WebhooksConfig configuration,
            UnitOfWorkAwareProxyFactory unitOfWorkAwareProxyFactory,
            WebhookDeliveryQueueDao webhookDeliveryQueueDao,
            InstantSource instantSource,
            WebhookMessageSender webhookMessageSender
    ) {
        this.config = configuration.getWebhookMessageSendingQueueProcessorConfig();
        this.webhookMessagePollingService = unitOfWorkAwareProxyFactory.create(
                WebhookMessagePollingService.class,
                new Class[] { WebhookDeliveryQueueDao.class, SendAttempter.class },
                new Object[] { webhookDeliveryQueueDao, new SendAttempter(webhookDeliveryQueueDao, instantSource, webhookMessageSender, environment) }
        );

        scheduledExecutorService = environment
                .lifecycle()
                .scheduledExecutorService("webhook-message-sending-queue-process-%d")
                .threads(config.getNumberOfThreads())
                .build();
    }

    @Override
    public void start() {
        for (int i = 0; i < config.getNumberOfThreads(); i++){
            scheduledExecutorService.scheduleWithFixedDelay(
                    this::process,
                    config.getInitialDelayInMilliseconds(),
                    config.getThreadDelayInMilliseconds(),
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private void process() {
        try {
            MDC.put(JOB_BATCH_ID, UUID.randomUUID().toString());
            webhookMessagePollingService.pollWebhookMessageQueue();
        } catch (Exception e) {
            LOGGER.error("Queue message to send poller thread exception", e);
        } finally {
            MDC.remove(JOB_BATCH_ID);
        }
    }

    @Override
    public void stop() {
        scheduledExecutorService.shutdown();
    }
}

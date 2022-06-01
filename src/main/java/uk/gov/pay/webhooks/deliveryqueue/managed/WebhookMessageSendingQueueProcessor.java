package uk.gov.pay.webhooks.deliveryqueue.managed;

import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import uk.gov.pay.webhooks.app.WebhookMessageSendingQueueProcessorConfig;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueDao;
import uk.gov.pay.webhooks.message.WebhookMessageSender;

import javax.inject.Inject;
import java.time.InstantSource;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebhookMessageSendingQueueProcessor implements Managed {
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
                .scheduledExecutorService("webhook-message-sending-queue-process")
                .threads(config.getNumberOfThreads())
                .build();
    }

    @Override
    public void start() {
        scheduledExecutorService.scheduleWithFixedDelay(
                webhookMessagePollingService::pollWebhookMessageQueue,
                config.getInitialDelayInMilliseconds(),
                config.getThreadDelayInMilliseconds(), 
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void stop() {
        scheduledExecutorService.shutdown();
    }
}

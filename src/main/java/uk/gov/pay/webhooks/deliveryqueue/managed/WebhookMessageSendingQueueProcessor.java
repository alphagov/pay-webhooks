package uk.gov.pay.webhooks.deliveryqueue.managed;

import io.dropwizard.lifecycle.Managed;
import io.dropwizard.setup.Environment;
import uk.gov.pay.webhooks.app.WebhookMessageSendingQueueProcessorConfig;
import uk.gov.pay.webhooks.app.WebhooksConfig;

import javax.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebhookMessageSendingQueueProcessor implements Managed {
    private final ScheduledExecutorService scheduledExecutorService;
    private final WebhookMessageSendingQueueProcessorConfig config;

    private final WebhookMessagePollingService webhookMessagePollingService;
    
    @Inject
    public WebhookMessageSendingQueueProcessor(Environment environment, WebhooksConfig configuration, WebhookMessagePollingService webhookMessagePollingService) {
        this.config = configuration.getWebhookMessageSendingQueueProcessorConfig();
        this.webhookMessagePollingService = webhookMessagePollingService;

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

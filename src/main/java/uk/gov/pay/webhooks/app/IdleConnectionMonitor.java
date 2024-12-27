package uk.gov.pay.webhooks.app;

import io.dropwizard.lifecycle.Managed;
import io.dropwizard.core.setup.Environment;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IdleConnectionMonitor implements Managed {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdleConnectionMonitor.class);
    private final WebhookMessageSendingQueueProcessorConfig config;
    private final ScheduledExecutorService scheduledExecutorService;
    private final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager;

    @Inject
    IdleConnectionMonitor(PoolingHttpClientConnectionManager poolingHttpClientConnectionManager,
                          WebhooksConfig configuration, Environment environment) {
        this.config = configuration.getWebhookMessageSendingQueueProcessorConfig();

        this.poolingHttpClientConnectionManager = poolingHttpClientConnectionManager;
        
        scheduledExecutorService = environment
                .lifecycle()
                .scheduledExecutorService("idle-connection-monitor-%d")
                .threads(1)
                .build();
    }

    @Override
    public void start() {
        if (config.isIdleConnectionMonitorEnabled()) {
            LOGGER.info("Starting IdleConnectionMonitor");
            scheduledExecutorService.scheduleWithFixedDelay(
                    this::process,
                    config.getIdleConnectionMonitorInitialDelayInMilliseconds(),
                    config.getIdleConnectionMonitorThreadDelayInMilliseconds(),
                    TimeUnit.MILLISECONDS
            );
        }
    }

    private void process() {
        try {
            poolingHttpClientConnectionManager.closeExpiredConnections();
            poolingHttpClientConnectionManager.closeIdleConnections(config.getConnectionPoolIdleConnectionTimeToLive().toSeconds(), TimeUnit.SECONDS);
        } catch (Exception ex) {
            LOGGER.error("Close Idle Connections thread exception", ex);
        }
    }
    
    @Override
    public void stop() {
        if (config.isIdleConnectionMonitorEnabled()) {
            LOGGER.info("Stopping IdleConnectionMonitor");
            scheduledExecutorService.shutdown();
        }
    }
}

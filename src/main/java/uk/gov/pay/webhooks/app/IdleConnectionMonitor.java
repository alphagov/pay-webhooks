package uk.gov.pay.webhooks.app;

import io.dropwizard.lifecycle.Managed;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class IdleConnectionMonitor implements Managed {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdleConnectionMonitor.class);
    private final boolean idleConnectionMonitorIsEnabled;

    IdleConnectionMonitorThread staleMonitor;

    @Inject
    IdleConnectionMonitor(PoolingHttpClientConnectionManager poolingHttpClientConnectionManager,
                          WebhooksConfig configuration) {
        WebhookMessageSendingQueueProcessorConfig config = configuration.getWebhookMessageSendingQueueProcessorConfig();
        // todo: idleConnectionMonitorIsEnabled = getFromWebhooksConfig
        idleConnectionMonitorIsEnabled = true;
        staleMonitor
                = new IdleConnectionMonitorThread(
                        poolingHttpClientConnectionManager,
                config.getConnectionPoolIdleConnectionTimeToLive().toSeconds(),
                1000 // todo: add new env variable
        );
    }

    @Override
    public void start() {
        if (idleConnectionMonitorIsEnabled) {
            LOGGER.info("Starting IdleConnectionMonitor");
            staleMonitor.start();
        }
    }

    @Override
    public void stop() {
        if (idleConnectionMonitorIsEnabled) {
            LOGGER.info("Stopping IdleConnectionMonitor");
            staleMonitor.shutdown();
        }
    }
}

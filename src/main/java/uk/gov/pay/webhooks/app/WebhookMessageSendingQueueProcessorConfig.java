package uk.gov.pay.webhooks.app;

import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class WebhookMessageSendingQueueProcessorConfig extends Configuration {

    @Valid
    @NotNull
    private int threadDelayInMilliseconds;

    @Valid
    @NotNull
    private int numberOfThreads;

    @Valid
    @NotNull
    private int initialDelayInMilliseconds;

    @Valid
    @NotNull
    private int httpClientConnectionPoolSize;

    @NotNull
    private Duration connectionPoolIdleConnectionTimeToLive;
            
    @NotNull
    private Duration connectionPoolTimeToLive;
    
    @NotNull
    private Duration requestTimeout;

    @NotNull
    private boolean idleConnectionMonitorEnabled;
    
    @NotNull
    private int idleConnectionMonitorInitialDelayInMilliseconds;
    
    @NotNull
    private int idleConnectionMonitorThreadDelayInMilliseconds;

    public int getThreadDelayInMilliseconds() {
        return threadDelayInMilliseconds;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public int getInitialDelayInMilliseconds() {
        return initialDelayInMilliseconds;
    }

    public int getHttpClientConnectionPoolSize() {
        return httpClientConnectionPoolSize;
    }

    public Duration getConnectionPoolIdleConnectionTimeToLive() {
        return connectionPoolIdleConnectionTimeToLive;
    }
    
    public Duration getConnectionPoolTimeToLive() {
        return connectionPoolTimeToLive;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public boolean isIdleConnectionMonitorEnabled() {
        return idleConnectionMonitorEnabled;
    }
    
    public int getIdleConnectionMonitorInitialDelayInMilliseconds() {
        return idleConnectionMonitorInitialDelayInMilliseconds;
    }

    public int getIdleConnectionMonitorThreadDelayInMilliseconds() {
        return idleConnectionMonitorThreadDelayInMilliseconds;
    }
}

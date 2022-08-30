package uk.gov.pay.webhooks.app;

import io.dropwizard.Configuration;
import io.dropwizard.util.Duration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class QueueMessageReceiverConfig extends Configuration {

    @Valid
    private boolean backgroundProcessingEnabled;

    @Valid
    @NotNull
    private int threadDelayInMilliseconds;

    @Valid
    @NotNull
    private int numberOfThreads;

    @Valid
    @NotNull
    private int messageRetryDelayInSeconds;
    @NotNull
    private Duration connectionPoolTimeToLive;
    @NotNull
    private Duration requestTimeout;

    public int getThreadDelayInMilliseconds() {
        return threadDelayInMilliseconds;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public int getMessageRetryDelayInSeconds() {
        return messageRetryDelayInSeconds;
    }

    public boolean isBackgroundProcessingEnabled() { return backgroundProcessingEnabled; }

    public Duration getConnectionPoolTimeToLive() {
        return connectionPoolTimeToLive;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }
}

package uk.gov.pay.webhooks.app;

import io.dropwizard.core.Configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

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
}

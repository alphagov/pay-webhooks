package uk.gov.pay.webhooks.app;

import io.dropwizard.Configuration;

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

    public int getThreadDelayInMilliseconds() {
        return threadDelayInMilliseconds;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public int getInitialDelayInMilliseconds() {
        return initialDelayInMilliseconds;
    }
}

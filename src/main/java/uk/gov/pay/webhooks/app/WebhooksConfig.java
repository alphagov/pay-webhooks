package uk.gov.pay.webhooks.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Set;

public class WebhooksConfig extends Configuration {
    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
        this.database = dataSourceFactory;
    }

    @NotNull
    @JsonProperty("ledgerBaseURL")
    private String ledgerBaseUrl;

    public String getLedgerBaseUrl() {
        return ledgerBaseUrl;
    }

    @Valid
    @NotNull
    @JsonProperty("sqsConfig")
    private SqsConfig sqsConfig;

    public SqsConfig getSqsConfig() {
        return sqsConfig;
    }

    @NotNull
    @JsonProperty("queueMessageReceiverConfig")
    private QueueMessageReceiverConfig queueMessageReceiverConfig;

    @NotNull
    private InternalRestClientConfig internalRestClientConfig;

    public QueueMessageReceiverConfig getQueueMessageReceiverConfig() {
        return queueMessageReceiverConfig;
    }

    public InternalRestClientConfig getInternalRestClientConfig() {
        return internalRestClientConfig;
    }

    @NotNull
    @JsonProperty("webhookMessageDeletionConfig")
    private WebhookMessageDeletionConfig webhookMessageDeletionConfig;

    public WebhookMessageDeletionConfig getWebhookMessageDeletionConfig() {
        return webhookMessageDeletionConfig;
    }

    @NotNull
    @JsonProperty("webhookMessageSendingQueueProcessorConfig")
    private WebhookMessageSendingQueueProcessorConfig webhookMessageSendingQueueProcessorConfig;

    public WebhookMessageSendingQueueProcessorConfig getWebhookMessageSendingQueueProcessorConfig() {
        return webhookMessageSendingQueueProcessorConfig;
    }
    
    @NotNull
    @JsonProperty("graphiteHost")
    private String graphiteHost;
    
    public String getGraphiteHost() {
        return graphiteHost;
    }

    @NotNull
    @JsonProperty("graphitePort")
    private String graphitePort;
    
    public String getGraphitePort() {
        return graphitePort;
    }

    @NotNull
    @JsonProperty("liveDataAllowDomains")
    private Set<String> liveDataAllowDomains;

    public Set<String> getLiveDataAllowDomains() {
        return liveDataAllowDomains;
    }
}

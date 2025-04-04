package uk.gov.pay.webhooks.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import io.dropwizard.db.DataSourceFactory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.Optional;
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

    @JsonProperty("ecsContainerMetadataUriV4")
    private URI ecsContainerMetadataUriV4;

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
    @JsonProperty("liveDataAllowDomains")
    private Set<String> liveDataAllowDomains;

    public Set<String> getLiveDataAllowDomains() {
        return liveDataAllowDomains;
    }

    public Optional<URI> getEcsContainerMetadataUriV4() {
        return Optional.ofNullable(ecsContainerMetadataUriV4);
    }
}

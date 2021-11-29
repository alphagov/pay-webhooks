package uk.gov.pay.webhooks.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import uk.gov.pay.webhooks.app.config.SqsConfig;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

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

    @Valid
    @NotNull
    @JsonProperty("sqsConfig")
    private SqsConfig sqsConfig;

    public String getLedgerBaseUrl() {
        return ledgerBaseUrl;
    }

    @JsonProperty("sqs")
    public SqsConfig getSqsConfig() {
        return sqsConfig;
    }
}

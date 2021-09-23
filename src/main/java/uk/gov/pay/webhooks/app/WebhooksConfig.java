package uk.gov.pay.webhooks.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import uk.gov.pay.webhooks.app.config.JpaConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class WebhooksConfig extends Configuration {
    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @Valid
    @NotNull
    private JpaConfiguration jpaConfiguration;

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }
    
    @JsonProperty("jpa")
    public JpaConfiguration getJpaConfiguration() {
        return jpaConfiguration;
    }

    @JsonProperty("database")
    public void setDataSourceFactory(DataSourceFactory dataSourceFactory) {
        this.database = dataSourceFactory;
    }
}

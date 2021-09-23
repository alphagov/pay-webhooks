package uk.gov.pay.webhooks.app;

import com.google.inject.AbstractModule;
import com.google.inject.persist.jpa.JpaPersistModule;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.setup.Environment;
import uk.gov.pay.webhooks.app.config.JpaConfiguration;

import java.util.Properties;

public class WebhooksModule extends AbstractModule {
    private final WebhooksConfig configuration;
    private final Environment environment;

    public WebhooksModule(final WebhooksConfig configuration, final Environment environment) {
        this.configuration = configuration;
        this.environment = environment;
    }

    @Override
    protected void configure() {
        bind(WebhooksConfig.class).toInstance(configuration);
        bind(Environment.class).toInstance(environment);
        
        install(jpaModule(configuration));
    }

    private JpaPersistModule jpaModule(WebhooksConfig configuration) {
        DataSourceFactory dbConfig = configuration.getDataSourceFactory();
        final Properties properties = new Properties();
        properties.put("javax.persistence.jdbc.driver", dbConfig.getDriverClass());
        properties.put("javax.persistence.jdbc.url", dbConfig.getUrl());
        properties.put("javax.persistence.jdbc.user", dbConfig.getUser());
        properties.put("javax.persistence.jdbc.password", dbConfig.getPassword());

        JpaConfiguration jpaConfiguration = configuration.getJpaConfiguration();
        properties.put("eclipselink.logging.level", jpaConfiguration.getJpaLoggingLevel());
        properties.put("eclipselink.logging.level.sql", jpaConfiguration.getSqlLoggingLevel());
        properties.put("eclipselink.query-results-cache", jpaConfiguration.getCacheSharedDefault());
        properties.put("eclipselink.cache.shared.default", jpaConfiguration.getCacheSharedDefault());
        properties.put("eclipselink.ddl-generation.output-mode", jpaConfiguration.getDdlGenerationOutputMode());

        final JpaPersistModule jpaModule = new JpaPersistModule("WebhooksUnit");
        jpaModule.properties(properties);

        return jpaModule;
    }
}

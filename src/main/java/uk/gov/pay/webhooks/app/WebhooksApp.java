package uk.gov.pay.webhooks.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.pay.webhooks.healthcheck.HealthCheckResource;
import uk.gov.pay.webhooks.healthcheck.Ping;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.webhook.resource.WebhookResource;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.service.payments.commons.utils.healthchecks.DatabaseHealthCheck;

public class WebhooksApp extends Application<WebhooksConfig> {
    public static void main(String[] args) throws Exception {
        new WebhooksApp().run(args);
    }
    
    private HibernateBundle<WebhooksConfig> hibernate = new HibernateBundle<>(
            WebhookEntity.class,
            EventTypeEntity.class
    ) {
        @Override
        public DataSourceFactory getDataSourceFactory(WebhooksConfig configuration) {
            return configuration.getDataSourceFactory();
        }
    };

    @Override
    public void run(WebhooksConfig configuration,
                    Environment environment) {
        final Injector injector = Guice.createInjector(new WebhooksModule(configuration, environment, hibernate));

        environment.healthChecks().register("ping", new Ping());
        environment.healthChecks().register("database", new DatabaseHealthCheck(configuration.getDataSourceFactory()));
        environment.jersey().register(injector.getInstance(HealthCheckResource.class));
        environment.jersey().register(injector.getInstance(WebhookResource.class));
    }

    @Override
    public void initialize(Bootstrap<WebhooksConfig> bootstrap){
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false))
        );

        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public DataSourceFactory getDataSourceFactory(WebhooksConfig configuration) {
                return configuration.getDataSourceFactory();
            }
        });

        bootstrap.addBundle(hibernate);
    }


}

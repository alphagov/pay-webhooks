package uk.gov.pay.webhooks.app;

import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.pay.webhooks.healthcheck.HealthCheckResource;
import uk.gov.pay.webhooks.healthcheck.Ping;
import com.google.inject.Guice;
import com.google.inject.Injector;
import uk.gov.pay.webhooks.webhook.WebhookResource;
import uk.gov.service.payments.commons.utils.healthchecks.DatabaseHealthCheck;

public class WebhooksApp extends Application<WebhooksConfig> {
    public static void main(String[] args) throws Exception {
        new WebhooksApp().run(args);
    }

@Override
public void run(WebhooksConfig configuration,
                Environment environment) {
        final Injector injector = Guice.createInjector(new WebhooksModule(configuration, environment));

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
    }

}

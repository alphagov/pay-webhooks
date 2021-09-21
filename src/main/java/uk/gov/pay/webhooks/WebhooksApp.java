package uk.gov.pay.webhooks;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import uk.gov.pay.webhooks.healthcheck.HealthCheckResource;
import uk.gov.pay.webhooks.healthcheck.Ping;

public class WebhooksApp extends Application<WebhooksConfiguration> {
    public static void main(String[] args) throws Exception {
        new WebhooksApp().run(args);
    }

@Override
public void run(WebhooksConfiguration configuration,
                Environment environment) {
    final HealthCheckResource resource = new HealthCheckResource(environment);
    environment.jersey().register(resource);
    environment.healthChecks().register("ping", new Ping());
    
    }

    @Override
    public void initialize(Bootstrap<WebhooksConfiguration> bootstrap){
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false))
        );

        bootstrap.addBundle(new MigrationsBundle<>() {
            @Override
            public DataSourceFactory getDataSourceFactory(WebhooksConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
    }

}

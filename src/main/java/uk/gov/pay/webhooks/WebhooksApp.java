package uk.gov.pay.webhooks;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
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




}

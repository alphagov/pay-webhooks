package uk.gov.pay.webhooks.app;

import com.google.inject.AbstractModule;
import io.dropwizard.setup.Environment;

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
    }
}

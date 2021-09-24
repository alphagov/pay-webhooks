package uk.gov.pay.webhooks.app;

import com.google.inject.AbstractModule;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Environment;
import org.hibernate.SessionFactory;

public class WebhooksModule extends AbstractModule {
    private final WebhooksConfig configuration;
    private final Environment environment;
    private final HibernateBundle<WebhooksConfig> hibernate;

    public WebhooksModule(final WebhooksConfig configuration, final Environment environment, HibernateBundle<WebhooksConfig> hibernate) {
        this.configuration = configuration;
        this.environment = environment;
        this.hibernate = hibernate;
    }

    @Override
    protected void configure() {
        bind(WebhooksConfig.class).toInstance(configuration);
        bind(Environment.class).toInstance(environment);
        bind(SessionFactory.class).toInstance(hibernate.getSessionFactory());
    }
}

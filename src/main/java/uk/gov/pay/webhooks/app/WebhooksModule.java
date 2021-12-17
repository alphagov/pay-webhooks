package uk.gov.pay.webhooks.app;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Environment;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.util.IdGenerator;

import javax.inject.Singleton;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.InstantSource;

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

    @Provides
    @Singleton
    public InstantSource instantSource() {
        return InstantSource.system();
    }

    @Provides
    @Singleton
    public IdGenerator externalIdGenerator() {
        return new IdGenerator();
    }

    @Provides
    @Singleton
    public HttpClient httpClient() {
        return HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

}

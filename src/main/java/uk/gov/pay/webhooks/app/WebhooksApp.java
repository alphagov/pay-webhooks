package uk.gov.pay.webhooks.app;

import com.codahale.metrics.graphite.GraphiteReporter;
import com.codahale.metrics.graphite.GraphiteSender;
import com.codahale.metrics.graphite.GraphiteUDP;
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
import uk.gov.pay.webhooks.app.filters.LoggingMDCRequestFilter;
import uk.gov.pay.webhooks.app.filters.LoggingMDCResponseFilter;
import uk.gov.pay.webhooks.deliveryqueue.dao.WebhookDeliveryQueueEntity;
import uk.gov.pay.webhooks.deliveryqueue.managed.WebhookMessageSendingQueueProcessor;
import uk.gov.pay.webhooks.eventtype.dao.EventTypeEntity;
import uk.gov.pay.webhooks.healthcheck.HealthCheckResource;
import uk.gov.pay.webhooks.healthcheck.Ping;
import uk.gov.pay.webhooks.healthcheck.SQSHealthCheck;
import uk.gov.pay.webhooks.message.dao.entity.WebhookMessageEntity;
import uk.gov.pay.webhooks.queue.QueueMessageReceiver;
import uk.gov.pay.webhooks.webhook.dao.entity.WebhookEntity;
import uk.gov.pay.webhooks.webhook.exception.ValidationExceptionMapper;
import uk.gov.pay.webhooks.webhook.exception.WebhookExceptionMapper;
import uk.gov.pay.webhooks.webhook.resource.WebhookResource;
import uk.gov.service.payments.commons.utils.healthchecks.DatabaseHealthCheck;
import uk.gov.service.payments.commons.utils.metrics.DatabaseMetricsService;
import uk.gov.service.payments.logging.GovUkPayDropwizardRequestJsonLogLayoutFactory;
import uk.gov.service.payments.logging.LoggingFilter;
import uk.gov.service.payments.logging.LogstashConsoleAppenderFactory;

import java.util.concurrent.TimeUnit;

import static java.util.EnumSet.of;
import static javax.servlet.DispatcherType.REQUEST;

public class WebhooksApp extends Application<WebhooksConfig> {
    public static void main(String[] args) throws Exception {
        new WebhooksApp().run(args);
    }
    
    private static final String SERVICE_METRICS_NODE = "webhooks";
    private static final int GRAPHITE_SENDING_PERIOD_SECONDS = 10;

    private final HibernateBundle<WebhooksConfig> hibernate = new HibernateBundle<>(
            WebhookEntity.class,
            EventTypeEntity.class,
            WebhookMessageEntity.class,
            WebhookDeliveryQueueEntity.class
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

        environment.servlets().addFilter("LoggingFilter", new LoggingFilter())
                .addMappingForUrlPatterns(of(REQUEST), true, "/v1/*");
        environment.jersey().register(injector.getInstance(LoggingMDCRequestFilter.class));
        environment.jersey().register(injector.getInstance(LoggingMDCResponseFilter.class));

        environment.healthChecks().register("ping", new Ping());
        environment.healthChecks().register("database", new DatabaseHealthCheck(configuration.getDataSourceFactory()));
        environment.healthChecks().register("sqsQueue", injector.getInstance(SQSHealthCheck.class));
        environment.jersey().register(injector.getInstance(HealthCheckResource.class));
        environment.jersey().register(injector.getInstance(WebhookResource.class));

        environment.jersey().register(new ValidationExceptionMapper());
        environment.jersey().register(new WebhookExceptionMapper());

        if (configuration.getQueueMessageReceiverConfig().isBackgroundProcessingEnabled()) {
            environment.lifecycle().manage(injector.getInstance(QueueMessageReceiver.class));
            environment.lifecycle().manage(injector.getInstance(WebhookMessageSendingQueueProcessor.class));
        }
        initialiseMetrics(configuration, environment);
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
        bootstrap.getObjectMapper().getSubtypeResolver().registerSubtypes(LogstashConsoleAppenderFactory.class);
        bootstrap.getObjectMapper().getSubtypeResolver().registerSubtypes(GovUkPayDropwizardRequestJsonLogLayoutFactory.class);
    }

    private void initialiseMetrics(WebhooksConfig configuration, Environment environment) {
        DatabaseMetricsService metricsService = new DatabaseMetricsService(configuration.getDataSourceFactory(), environment.metrics(), "webhooks");

        environment
                .lifecycle()
                .scheduledExecutorService("metricscollector")
                .threads(1)
                .build()
                .scheduleAtFixedRate(metricsService::updateMetricData, 0, GRAPHITE_SENDING_PERIOD_SECONDS / 2, TimeUnit.SECONDS);

        GraphiteSender graphiteUDP = new GraphiteUDP(configuration.getGraphiteHost(), Integer.parseInt(configuration.getGraphitePort()));
        GraphiteReporter.forRegistry(environment.metrics())
                .prefixedWith(SERVICE_METRICS_NODE)
                .convertRatesTo(TimeUnit.MINUTES)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(graphiteUDP)
                .start(GRAPHITE_SENDING_PERIOD_SECONDS, TimeUnit.SECONDS);
    }

}

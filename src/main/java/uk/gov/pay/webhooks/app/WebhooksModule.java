package uk.gov.pay.webhooks.app;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.setup.Environment;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.message.WebhookMessageSender;
import uk.gov.pay.webhooks.message.WebhookMessageSignatureGenerator;
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

    @Provides
    @Singleton
    public WebhookMessageSender webhookMessageSender() {
        return new WebhookMessageSender(httpClient(), new ObjectMapper().registerModule(new Jdk8Module()),
                new WebhookMessageSignatureGenerator());
    }

    @Provides
    public AmazonSQS sqsClient(WebhooksConfig webhooksConfig) {
        AmazonSQSClientBuilder clientBuilder = AmazonSQSClientBuilder
                .standard();
        if (webhooksConfig.getSqsConfig().isNonStandardServiceEndpoint()) {
            // build static credentials in a local environment
            BasicAWSCredentials basicAWSCredentials = new BasicAWSCredentials(
                    webhooksConfig.getSqsConfig().getAccessKey(),
                    webhooksConfig.getSqsConfig().getSecretKey());

            clientBuilder
                    .withCredentials(new AWSStaticCredentialsProvider(basicAWSCredentials))
                    .withEndpointConfiguration(
                            new AwsClientBuilder.EndpointConfiguration(
                                    webhooksConfig.getSqsConfig().getEndpoint(),
                                    webhooksConfig.getSqsConfig().getRegion())
                    );
        } else {
            // AWS SDK will use the default provider chain to get credentials from ECS
            clientBuilder.withRegion(webhooksConfig.getSqsConfig().getRegion());
        }

        return clientBuilder.build();
    }
}

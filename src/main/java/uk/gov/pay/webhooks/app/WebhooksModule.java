package uk.gov.pay.webhooks.app;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import io.dropwizard.setup.Environment;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.queue.EventMessageHandler;
import uk.gov.pay.webhooks.queue.managed.QueueMessageReceiver;

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
    public AmazonSQS sqsClient(WebhooksConfig webhooksConfig) {
        AmazonSQSClientBuilder clientBuilder = AmazonSQSClientBuilder
                .standard();

        if (webhooksConfig.getSqsConfig().isNonStandardServiceEndpoint()) {

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
            // uses AWS SDK's DefaultAWSCredentialsProviderChain to obtain credentials
            clientBuilder.withRegion(webhooksConfig.getSqsConfig().getRegion());
        }

        return clientBuilder.build();
    }
}

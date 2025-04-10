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
import io.dropwizard.core.setup.Environment;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.hibernate.SessionFactory;
import uk.gov.pay.webhooks.message.WebhookMessageSignatureGenerator;
import uk.gov.pay.webhooks.util.IdGenerator;

import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import java.time.InstantSource;
import java.util.concurrent.TimeUnit;

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
    public Client internalRestClient() {
        return InternalRestClientFactory.buildClient(configuration.getInternalRestClientConfig());
    }

    @Provides
    @Singleton
    public WebhookMessageDeletionConfig webhookMessageDeletionConfig() {
        return configuration.getWebhookMessageDeletionConfig();
    }

    @Singleton
    @Provides
    public PoolingHttpClientConnectionManager getConnectionPoolManager() {
        int connectionPoolSize = configuration.getWebhookMessageSendingQueueProcessorConfig().getHttpClientConnectionPoolSize();
        PoolingHttpClientConnectionManager poolingConnManager
                = new PoolingHttpClientConnectionManager();
        poolingConnManager.setMaxTotal(connectionPoolSize);
        poolingConnManager.setDefaultMaxPerRoute(connectionPoolSize);

        return poolingConnManager;
    }

    @Provides
    @Singleton
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager poolingConnManager) {
        var timeoutInMillis = Math.toIntExact(configuration.getWebhookMessageSendingQueueProcessorConfig().getRequestTimeout().toMilliseconds());
        var config = RequestConfig.custom()
                .setConnectTimeout(timeoutInMillis)
                .setConnectionRequestTimeout(timeoutInMillis)
                .setSocketTimeout(timeoutInMillis)
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();

        var sslsf = new SSLConnectionSocketFactory(
                SSLContexts.createDefault(),
                new String[]{"TLSv1.2", "TLSv1.3"},
                null,
                SSLConnectionSocketFactory.getDefaultHostnameVerifier()
        );

        return HttpClientBuilder.create()
                .useSystemProperties()
                .setConnectionTimeToLive(configuration.getWebhookMessageSendingQueueProcessorConfig().getConnectionPoolTimeToLive().toSeconds(), TimeUnit.SECONDS)
                .setSSLSocketFactory(sslsf)
                .setDefaultRequestConfig(config)
                .setConnectionManager(poolingConnManager)
                .build();
    }

    @Provides
    @Singleton
    public WebhookMessageSignatureGenerator webhookMessageSignatureGenerator() {
        return new WebhookMessageSignatureGenerator();
    }

    @Provides
    @Singleton
    public UnitOfWorkAwareProxyFactory unitOfWorkAwareProxyFactory() {
        return new UnitOfWorkAwareProxyFactory(hibernate);
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

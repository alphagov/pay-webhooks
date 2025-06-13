package uk.gov.pay.webhooks.app;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.hibernate.SessionFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import uk.gov.pay.webhooks.message.HttpPostFactory;
import uk.gov.pay.webhooks.message.WebhookMessageSignatureGenerator;
import uk.gov.pay.webhooks.util.IdGenerator;

import java.net.URI;
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
    public HttpPostFactory httpPostFactory() {
        return new HttpPostFactory();
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
    public SqsClient sqsClient(WebhooksConfig webhooksConfig) {
        SqsClientBuilder clientBuilder = SqsClient.builder();
        if (webhooksConfig.getSqsConfig().isNonStandardServiceEndpoint()) {
            // build static credentials in a local environment
            AwsBasicCredentials basicAWSCredentials = AwsBasicCredentials
                    .create(webhooksConfig
                                    .getSqsConfig()
                                    .getAccessKey(),
                            webhooksConfig.getSqsConfig().getSecretKey());

            clientBuilder
                    .credentialsProvider(StaticCredentialsProvider.create(basicAWSCredentials))
                    .endpointOverride(URI.create(webhooksConfig.getSqsConfig().getEndpoint()))
                    .region(Region.of(webhooksConfig.getSqsConfig().getRegion()));
        } else {
            // AWS SDK will use the default provider chain to get credentials from ECS
            clientBuilder.region(Region.of(webhooksConfig.getSqsConfig().getRegion()));
        }

        return clientBuilder.build();
    }
}

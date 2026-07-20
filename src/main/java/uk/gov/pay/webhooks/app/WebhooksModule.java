package uk.gov.pay.webhooks.app;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.UnitOfWorkAwareProxyFactory;
import jakarta.inject.Singleton;
import jakarta.ws.rs.client.Client;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.hibernate.SessionFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import uk.gov.pay.webhooks.message.HttpPostFactory;
import uk.gov.pay.webhooks.message.WebhookMessageSignatureGenerator;
import uk.gov.pay.webhooks.util.IdGenerator;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
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
        SSLConnectionSocketFactory sslConnectionSocketFactory;
        try {
            sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                    SSLContext.getDefault(),
                    new String[]{"TLSv1.2", "TLSv1.3"},
                    null,
                    new DefaultHostnameVerifier()
            );
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unable to create SSL connection socket factory", e);
        }
        
        PoolingHttpClientConnectionManager poolingConnManager = PoolingHttpClientConnectionManagerBuilder
                .create()
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .setMaxConnTotal(connectionPoolSize)
                .setMaxConnPerRoute(connectionPoolSize)
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.ofMinutes(1))
                        .build())
                .build();

        return poolingConnManager;
    }

    @Provides
    @Singleton
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager poolingConnManager) {
        var timeoutInMillis = Math.toIntExact(configuration.getWebhookMessageSendingQueueProcessorConfig().getRequestTimeout().toMilliseconds());
        var config = RequestConfig.custom()
                .setConnectTimeout(Timeout.of(timeoutInMillis, TimeUnit.MILLISECONDS))
                .setConnectionRequestTimeout(Timeout.of(timeoutInMillis, TimeUnit.MILLISECONDS))
                .setResponseTimeout(Timeout.of(timeoutInMillis, TimeUnit.MILLISECONDS))
                .setConnectionKeepAlive(Timeout.of(configuration.getWebhookMessageSendingQueueProcessorConfig().getConnectionPoolTimeToLive().toSeconds(), TimeUnit.SECONDS))
                .setCookieSpec(StandardCookieSpec.STRICT)
                .build();

        return HttpClientBuilder.create()
                .setConnectionManager(poolingConnManager)
                .useSystemProperties()
                .setDefaultRequestConfig(config)
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
        SqsClientBuilder clientBuilder = SqsClient
                .builder()
                .httpClient(Apache5HttpClient.create());
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

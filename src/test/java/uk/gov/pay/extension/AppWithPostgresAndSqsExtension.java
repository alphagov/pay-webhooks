package uk.gov.pay.extension;

import com.amazonaws.services.sqs.AmazonSQS;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.pay.rule.PostgresTestDocker;
import uk.gov.pay.rule.SqsTestDocker;
import uk.gov.pay.webhooks.app.WebhooksApp;
import uk.gov.pay.webhooks.app.WebhooksConfig;
import uk.gov.service.payments.commons.testing.port.PortFactory;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static uk.gov.pay.rule.PostgresTestDocker.getConnectionUrl;
import static uk.gov.pay.rule.PostgresTestDocker.getDbPassword;
import static uk.gov.pay.rule.PostgresTestDocker.getDbUsername;

public class AppWithPostgresAndSqsExtension implements BeforeAllCallback, AfterAllCallback {

    private static final Logger logger = LoggerFactory.getLogger(AppWithPostgresAndSqsExtension.class);

    private static final String CONFIG_PATH = resourceFilePath("config/test-config.yaml");
    private final Jdbi jdbi;
    private final DropwizardAppExtension<WebhooksConfig> dropwizardAppExtension;

    static final int wireMockPort = PortFactory.findFreePort();

    private final AmazonSQS sqsClient;

    public AppWithPostgresAndSqsExtension() {
        this(new ConfigOverride[0]);
    }

    public AppWithPostgresAndSqsExtension(ConfigOverride... configOverrides) {
        PostgresTestDocker.getOrCreate();
        sqsClient = SqsTestDocker.initialise("event-queue");

        ConfigOverride[] newConfigOverrides = overrideDatabaseConfig(configOverrides);
        newConfigOverrides = overrideSqsConfig(newConfigOverrides);
        newConfigOverrides = overrideUrlsConfig(newConfigOverrides);

        dropwizardAppExtension = new DropwizardAppExtension<>(WebhooksApp.class,
                CONFIG_PATH, newConfigOverrides);

        try {
            // starts dropwizard application. This is required as we don't use DropwizardExtensionsSupport (which starts application)
            // due to config overrides we need at runtime for database, sqs and any custom configuration needed for tests
            dropwizardAppExtension.before();
        } catch (Exception e) {
            logger.error("Exception starting application - {}", e.getMessage());
            throw new RuntimeException(e);
        }

        jdbi = Jdbi.create(getConnectionUrl(), getDbUsername(), getDbPassword());
        jdbi.installPlugin(new SqlObjectPlugin());
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        dropwizardAppExtension.getApplication().run("db", "migrate", CONFIG_PATH);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        dropwizardAppExtension.after();
    }

    private ConfigOverride[] overrideDatabaseConfig(ConfigOverride[] configOverrides) {
        List<ConfigOverride> newConfigOverride = newArrayList(configOverrides);
        newConfigOverride.add(config("database.url", getConnectionUrl()));
        newConfigOverride.add(config("database.user", getDbUsername()));
        newConfigOverride.add(config("database.password", getDbPassword()));
        return newConfigOverride.toArray(new ConfigOverride[0]);
    }

    private ConfigOverride[] overrideSqsConfig(ConfigOverride[] configOverrides) {
        List<ConfigOverride> newConfigOverride = newArrayList(configOverrides);
        newConfigOverride.add(config("sqsConfig.eventQueueUrl", SqsTestDocker.getQueueUrl("event-queue")));
        newConfigOverride.add(config("sqsConfig.endpoint", SqsTestDocker.getEndpoint()));
        return newConfigOverride.toArray(new ConfigOverride[0]);
    }

    private ConfigOverride[] overrideUrlsConfig(ConfigOverride[] configOverrides) {
        List<ConfigOverride> newConfigOverride = newArrayList(configOverrides);
        newConfigOverride.add(config("ledgerBaseURL", "http://localhost:" + getWireMockPort()));
        return newConfigOverride.toArray(new ConfigOverride[0]);
    }

    public DropwizardAppExtension<WebhooksConfig> getAppRule() {
        return dropwizardAppExtension;
    }

    public Jdbi getJdbi() {
        return jdbi;
    }

    public AmazonSQS getSqsClient() {
        return sqsClient;
    }

    public int getWireMockPort() {
        return wireMockPort;
    }
}

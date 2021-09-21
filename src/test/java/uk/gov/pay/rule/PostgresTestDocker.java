package uk.gov.pay.rule;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.junit.rules.ExternalResource;
import org.junit.rules.RuleChain;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import uk.gov.pay.webhooks.WebhooksConfiguration;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.sql.DriverManager.getConnection;

public class PostgresTestDocker {

    private static final Logger logger = LoggerFactory.getLogger(PostgresTestDocker.class);

    private static final String DB_NAME = "webhooks_test";
    private static final String DB_USERNAME = "test";
    private static final String DB_PASSWORD = "test";
    private static GenericContainer container;

    public static void getOrCreate() {
        try {
            if (container == null) {
                logger.info("Creating Postgres Container");

                container = new GenericContainer("postgres:13.4");
                container.addExposedPort(5432);

                container.addEnv("POSTGRES_USER", DB_USERNAME);
                container.addEnv("POSTGRES_PASSWORD", DB_PASSWORD);

                container.start();

                //todo: add DB health check
                Thread.sleep(5000);
                createDatabase(DB_NAME);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getConnectionUrl() {
        return "jdbc:postgresql://localhost:" + container.getMappedPort(5432) + "/";
    }

    public static void stopContainer() {
        container.stop();
        container = null;
    }

    private static void createDatabase(final String dbName) {
        final String dbUser = getDbUsername();

        try (Connection connection = getConnection(getConnectionUrl(), dbUser, getDbPassword())) {
            connection.createStatement().execute("CREATE DATABASE " + dbName + " WITH owner=" + dbUser + " TEMPLATE postgres");
            connection.createStatement().execute("GRANT ALL PRIVILEGES ON DATABASE " + dbName + " TO " + dbUser);
            connection.createStatement().execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static String getDbUri() {
        return getConnectionUrl() + DB_NAME;
    }

    public static String getDbPassword() {
        return DB_PASSWORD;
    }

    public static String getDbUsername() {
        return DB_USERNAME;
    }
}

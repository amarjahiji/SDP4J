package com.sdp4j.it;

import com.sdp4j.core.client.Sdp4jClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Base for integration tests that exercise SDP4J end-to-end against a real
 * PostgreSQL instance.
 *
 * <p>These complement the unit tests (which fake JDBC with proxies): only a real
 * database can verify that DDL actually applies, that catalog types match the
 * annotations, that the advisory-lock migration is concurrency-safe, and that
 * values round-trip with correct typing.
 *
 * <p>Where the database comes from is decided by {@link IntegrationDatabase}: an
 * external local Postgres if {@code SDP4J_IT_URL} is set, otherwise a
 * Testcontainers-managed container if Docker is available. If neither exists the
 * tests are <b>skipped</b> (via a JUnit assumption), so the build still passes.
 * The example entities in {@code com.sdp4j.demo.models} double as the schema
 * under test, and every test resets the {@code public} schema for isolation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class PostgresIntegrationTest {

    protected static final String ENTITY_PACKAGE = "com.sdp4j.demo.models";

    private static final IntegrationDatabase DATABASE = IntegrationDatabase.resolve();

    private final List<Sdp4jClient> openedClients = new ArrayList<>();

    @BeforeAll
    void requireDatabase() {
        Assumptions.assumeTrue(DATABASE.isAvailable(), DATABASE.unavailableReason());
    }

    @AfterAll
    void closePools() {
        // HikariDataSource is AutoCloseable; release pools created during the test.
        for (Sdp4jClient client : openedClients) {
            DataSource ds = client.getDataSource();
            if (ds instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception ignored) {
                    // best effort
                }
            }
        }
    }

    /** A client backed by its own connection pool against the resolved database. */
    protected Sdp4jClient newClient() {
        Sdp4jClient client = new Sdp4jClient(
                DATABASE.url(), DATABASE.username(), DATABASE.password(), ENTITY_PACKAGE);
        openedClients.add(client);
        return client;
    }

    /** Drops and recreates the public schema so each test starts from empty. */
    protected void resetSchema(DataSource dataSource) {
        execute(dataSource, "DROP SCHEMA public CASCADE", "CREATE SCHEMA public");
    }

    protected void execute(DataSource dataSource, String... statements) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute setup SQL", e);
        }
    }

    protected long scalarLong(DataSource dataSource, String sql) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read scalar for: " + sql, e);
        }
    }

    protected String scalarString(DataSource dataSource, String sql) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read scalar for: " + sql, e);
        }
    }

    protected boolean tableExists(DataSource dataSource, String tableName) {
        return scalarLong(dataSource,
                "SELECT count(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'public' AND table_name = '" + tableName + "'") == 1;
    }
}

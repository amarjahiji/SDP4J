package com.sdp4j.it;

import com.sdp4j.core.client.Sdp4jClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SM4J against a real Postgres: the schema is actually created, the column types
 * match the annotations (including {@code @Length} and {@code BigDecimal}/NUMERIC),
 * and re-running is a no-op.
 */
class MigrationIntegrationTest extends PostgresIntegrationTest {

    private Sdp4jClient client;
    private DataSource dataSource;

    @BeforeEach
    void setUp() {
        client = newClient();
        dataSource = client.getDataSource();
        resetSchema(dataSource);
    }

    @Test
    void firstRunCreatesEntityTablesAndBookkeeping() {
        client.getSm4j().executeMigration(List.of());

        assertTrue(tableExists(dataSource, "users"));
        assertTrue(tableExists(dataSource, "roles"));
        assertTrue(tableExists(dataSource, "products"));
        assertTrue(tableExists(dataSource, "migrations"));
        assertTrue(tableExists(dataSource, "migration_steps"));

        // Exactly one initial migration recorded.
        assertEquals(1, scalarLong(dataSource, "SELECT count(*) FROM migrations"));
        assertEquals(1, scalarLong(dataSource,
                "SELECT count(*) FROM migration_steps WHERE action = 'initial_schema_migration()'"));
    }

    @Test
    void lengthAndNumericAnnotationsProduceExpectedColumnTypes() {
        client.getSm4j().executeMigration(List.of());

        // @Length(120) -> VARCHAR(120); @Length(2000) -> VARCHAR(2000)
        assertEquals("character varying", columnDataType("products", "name"));
        assertEquals(120L, columnCharMaxLength("products", "name"));
        assertEquals(2000L, columnCharMaxLength("products", "description"));

        // BigDecimal -> NUMERIC, with @DefaultNumeric("0.00") default
        assertEquals("numeric", columnDataType("products", "price"));
        String priceDefault = scalarString(dataSource,
                "SELECT column_default FROM information_schema.columns "
                        + "WHERE table_name = 'products' AND column_name = 'price'");
        assertTrue(priceDefault != null && priceDefault.startsWith("0.00"),
                "expected price default to start with 0.00 but was " + priceDefault);
    }

    @Test
    void foreignKeyAndDefaultsAreApplied() {
        client.getSm4j().executeMigration(List.of());

        // roles.user_id references users(id)
        long fkCount = scalarLong(dataSource,
                "SELECT count(*) FROM information_schema.table_constraints "
                        + "WHERE table_name = 'roles' AND constraint_type = 'FOREIGN KEY'");
        assertTrue(fkCount >= 1, "expected a foreign key on roles");

        // users.is_active has a DEFAULT false from @DefaultFalse
        String isActiveDefault = scalarString(dataSource,
                "SELECT column_default FROM information_schema.columns "
                        + "WHERE table_name = 'users' AND column_name = 'is_active'");
        assertTrue(isActiveDefault != null && isActiveDefault.toLowerCase().contains("false"),
                "expected is_active default false but was " + isActiveDefault);
    }

    @Test
    void migrationIsIdempotent() {
        client.getSm4j().executeMigration(List.of());
        long tablesAfterFirst = scalarLong(dataSource,
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'");

        // Second run must not fail and must not re-create or duplicate anything.
        assertDoesNotThrow(() -> client.getSm4j().executeMigration(List.of()));

        long tablesAfterSecond = scalarLong(dataSource,
                "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'");
        assertEquals(tablesAfterFirst, tablesAfterSecond);
        // No additional migration row for a no-op run.
        assertEquals(1, scalarLong(dataSource, "SELECT count(*) FROM migrations"));
    }

    private String columnDataType(String table, String column) {
        return scalarString(dataSource,
                "SELECT data_type FROM information_schema.columns "
                        + "WHERE table_name = '" + table + "' AND column_name = '" + column + "'");
    }

    private long columnCharMaxLength(String table, String column) {
        return scalarLong(dataSource,
                "SELECT character_maximum_length FROM information_schema.columns "
                        + "WHERE table_name = '" + table + "' AND column_name = '" + column + "'");
    }
}

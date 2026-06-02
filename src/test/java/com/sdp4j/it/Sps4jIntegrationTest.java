package com.sdp4j.it;

import com.sdp4j.core.client.Sdp4jClient;
import com.sdp4j.sps4j.SPS4J;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPS4J against a real Postgres: named-parameter translation runs through an
 * actual driver — a repeated {@code :name} binds every occurrence, the Postgres
 * {@code ::} cast is left intact, batches execute, and {@code setNull} works.
 */
class Sps4jIntegrationTest extends PostgresIntegrationTest {

    private Sdp4jClient client;

    @BeforeEach
    void setUp() {
        client = newClient();
        resetSchema(client.getDataSource());
        client.getSm4j().executeMigration(List.of());
    }

    @Test
    void insertThenFetchByNamedParam() throws SQLException {
        String id = UUID.randomUUID().toString();
        insertUser(id, "Alice", "Smith", true);

        Connection con = client.getDataSource().getConnection();
        SPS4J ps = null;
        ResultSet rs = null;
        try {
            ps = client.getSps4j().connection(con)
                    .sql("SELECT first_name FROM users WHERE id = :id")
                    .set(":id", id);
            rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals("Alice", rs.getString("first_name"));
        } finally {
            client.closeResources(rs, con, ps);
        }
    }

    @Test
    void repeatedNamedParamBindsEveryOccurrence() throws SQLException {
        String id = UUID.randomUUID().toString();
        insertUser(id, "Match", "Other", true);

        Connection con = client.getDataSource().getConnection();
        SPS4J ps = null;
        ResultSet rs = null;
        try {
            // :name appears twice; a single set(...) must bind both positions.
            ps = client.getSps4j().connection(con)
                    .sql("SELECT id FROM users WHERE first_name = :name OR last_name = :name")
                    .set(":name", "Other");
            rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(id, rs.getString("id"));
        } finally {
            client.closeResources(rs, con, ps);
        }
    }

    @Test
    void postgresCastIsNotTreatedAsParameter() throws SQLException {
        String id = UUID.randomUUID().toString();
        insertUser(id, "Cast", "Test", true);

        Connection con = client.getDataSource().getConnection();
        SPS4J ps = null;
        ResultSet rs = null;
        try {
            // The :: cast must survive; only :active is a parameter.
            ps = client.getSps4j().connection(con)
                    .sql("SELECT id::text AS id FROM users WHERE is_active = :active")
                    .set(":active", true);
            rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(id, rs.getString("id"));
        } finally {
            client.closeResources(rs, con, ps);
        }
    }

    @Test
    void batchInsertExecutesEveryEntry() throws SQLException {
        Connection con = client.getDataSource().getConnection();
        SPS4J ps = null;
        try {
            ps = client.getSps4j().connection(con)
                    .sql("INSERT INTO users (id, first_name, last_name, is_active) "
                            + "VALUES (:id, :fn, :ln, :active)");
            ps.set(":id", UUID.randomUUID().toString()).set(":fn", "A").set(":ln", "One").set(":active", true).addBatch();
            ps.set(":id", UUID.randomUUID().toString()).set(":fn", "B").set(":ln", "Two").set(":active", true).addBatch();
            int[] counts = ps.executeBatch();
            assertEquals(2, counts.length);
        } finally {
            client.closeResources(con, ps);
        }

        assertEquals(2, scalarLong(client.getDataSource(), "SELECT count(*) FROM users"));
    }

    @Test
    void setNullBindsSqlNull() throws SQLException {
        String id = UUID.randomUUID().toString();
        insertUser(id, "Has", "LastName", true);

        Connection con = client.getDataSource().getConnection();
        SPS4J ps = null;
        try {
            ps = client.getSps4j().connection(con)
                    .sql("UPDATE users SET last_name = :ln WHERE id = :id")
                    .setNull(":ln", Types.VARCHAR)
                    .set(":id", id);
            assertEquals(1, ps.executeUpdate());
        } finally {
            client.closeResources(con, ps);
        }

        assertNull(scalarString(client.getDataSource(),
                "SELECT last_name FROM users WHERE id = '" + id + "'"));
    }

    private void insertUser(String id, String firstName, String lastName, boolean active) throws SQLException {
        Connection con = client.getDataSource().getConnection();
        SPS4J ps = null;
        try {
            ps = client.getSps4j().connection(con)
                    .sql("INSERT INTO users (id, first_name, last_name, is_active) "
                            + "VALUES (:id, :fn, :ln, :active)")
                    .set(":id", id)
                    .set(":fn", firstName)
                    .set(":ln", lastName)
                    .set(":active", active);
            ps.executeUpdate();
        } finally {
            client.closeResources(con, ps);
        }
    }
}

package com.sdp4j.it;

import com.sdp4j.core.client.Sdp4jClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the migration TOCTOU race (Issue 2): two application
 * instances starting at the same time against an empty database must not both
 * try to create the schema. The advisory lock is acquired before any catalog
 * read, so exactly one instance creates the schema and the other completes as a
 * no-op — neither throws.
 */
class ConcurrentMigrationIntegrationTest extends PostgresIntegrationTest {

    private DataSource adminDataSource;

    @BeforeEach
    void setUp() {
        // A throwaway client just to reset the schema before each run.
        adminDataSource = newClient().getDataSource();
        resetSchema(adminDataSource);
    }

    @Test
    void twoConcurrentMigrationsAgainstEmptyDatabaseBothSucceedAndCreateSchemaOnce() throws Exception {
        Sdp4jClient instanceA = newClient();
        Sdp4jClient instanceB = newClient();

        CyclicBarrier startLine = new CyclicBarrier(2);
        AtomicReference<Throwable> failureA = new AtomicReference<>();
        AtomicReference<Throwable> failureB = new AtomicReference<>();

        Thread threadA = migrationThread(instanceA, startLine, failureA);
        Thread threadB = migrationThread(instanceB, startLine, failureB);

        threadA.start();
        threadB.start();
        threadA.join();
        threadB.join();

        // Neither instance throws — the loser degrades to a no-op.
        assertNull(failureA.get(), () -> "instance A failed: " + failureA.get());
        assertNull(failureB.get(), () -> "instance B failed: " + failureB.get());

        // Schema created exactly once.
        assertTrue(tableExists(adminDataSource, "users"));
        assertTrue(tableExists(adminDataSource, "roles"));
        assertTrue(tableExists(adminDataSource, "products"));
        assertEquals(1, scalarLong(adminDataSource, "SELECT count(*) FROM migrations"));
        assertEquals(1, scalarLong(adminDataSource,
                "SELECT count(*) FROM migration_steps WHERE action = 'initial_schema_migration()'"));
    }

    private Thread migrationThread(Sdp4jClient client, CyclicBarrier startLine,
                                   AtomicReference<Throwable> failure) {
        return new Thread(() -> {
            try {
                startLine.await(); // maximise overlap
                client.getSm4j().executeMigration(List.of());
            } catch (Throwable t) {
                failure.set(t);
            }
        });
    }
}

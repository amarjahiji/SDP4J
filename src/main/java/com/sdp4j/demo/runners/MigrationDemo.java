package com.sdp4j.demo.runners;

import com.sdp4j.demo.ExampleDatabase;
import com.sdp4j.sm4j.SM4J;
import com.sdp4j.sm4j.metadata.MigrationStepMetadata;

import java.util.List;

/**
 * SM4J — schema migrations.
 *
 * <p>SM4J scans the configured package ({@code com.sdp4j.demo}) for
 * {@code @Table} classes and brings the database up to match them. The first run
 * creates every table (here: {@code users}, {@code roles}, {@code products})
 * plus the migration bookkeeping; subsequent runs apply only the additive
 * difference. Migrations are guarded by a Postgres advisory lock, so it is safe
 * to start several application instances at once — exactly one performs the work.
 */
public class MigrationDemo {

    private final SM4J sm4j;

    public MigrationDemo() {
        this.sm4j = ExampleDatabase.client().getSm4j();
    }

    public static void main(String[] args) {
        new MigrationDemo().run();
    }

    public void run() {
        createOrUpdateSchema();
        evolveSchemaExample();
    }

    private void createOrUpdateSchema() {
        // No drop/rename/alter steps: SM4J just reconciles the database with the
        // @Table entities. Idempotent — running it again is a no-op.
        sm4j.executeMigration(List.of());
        System.out.println("migration: schema is up to date");
    }

    private void evolveSchemaExample() {
        // Destructive or renaming changes are expressed explicitly as steps and
        // passed to the same entry point. Each step is recorded so it is applied
        // at most once. Shown here for reference; commented out so this demo
        // stays a pure create-the-schema example.
        //
        // List<MigrationStepMetadata> changes = List.of(
        //         sm4j.renameColumn("users", "last_name", "surname"),
        //         sm4j.alterColumnSetType("products", "description", "TEXT"),
        //         sm4j.dropColumn("roles", "obsolete_flag"));
        // sm4j.executeMigration(changes);

        List<MigrationStepMetadata> noChanges = List.of();
        sm4j.executeMigration(noChanges);
        System.out.println("migration: no pending schema changes");
    }
}

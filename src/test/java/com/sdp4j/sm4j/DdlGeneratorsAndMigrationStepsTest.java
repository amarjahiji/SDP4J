package com.sdp4j.sm4j;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sm4j.ddlgenerators.AddDdlsGenerator;
import com.sdp4j.sm4j.ddlgenerators.DropDdlsGenerator;
import com.sdp4j.sm4j.ddlgenerators.ModifyDdlsGenerator;
import com.sdp4j.sm4j.metadata.ColumnMetadata;
import com.sdp4j.sm4j.metadata.MigrationMetadata;
import com.sdp4j.sm4j.metadata.MigrationStepMetadata;
import com.sdp4j.sm4j.metadata.TableMetadata;
import com.sdp4j.testsupport.JdbcProxySupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DdlGeneratorsAndMigrationStepsTest {

    @Test
    void addGeneratorRendersColumnsForeignKeysAndIndexes() {
        ColumnMetadata roleId = column("role_id", "UUID");
        roleId.setForeignKeyTableReference("roles");
        roleId.setForeignKeyReferencedColumn("id");
        roleId.setForeignKeyOnDeleteAction("CASCADE");
        roleId.setIndex(true);
        TableMetadata table = new TableMetadata("users", Map.of(), true);
        table.setColumnMetadata(List.of(roleId));

        AddDdlsGenerator generator = new AddDdlsGenerator();

        assertEquals("ALTER TABLE users ADD COLUMN IF NOT EXISTS role_id UUID;",
                generator.generateAddColumn("users", roleId));
        assertTrue(generator.generateAddForeignKey("users", roleId).orElseThrow()
                .contains("ALTER TABLE users ADD CONSTRAINT fk_users_role_id FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE;"));
        assertEquals(List.of("CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);"),
                generator.generateIndicesDdls(table));
        assertEquals(1, generator.generateAddForeignKeys(table).size());
    }

    @Test
    void addForeignKeyIsEmptyWhenColumnHasNoReference() {
        assertTrue(new AddDdlsGenerator().generateAddForeignKey("users", column("name", "VARCHAR(255)")).isEmpty());
    }

    @Test
    void dropAndModifyGeneratorsRenderExpectedDdlAndValidateIdentifiers() {
        DropDdlsGenerator drop = new DropDdlsGenerator();
        ModifyDdlsGenerator modify = new ModifyDdlsGenerator();

        assertEquals("ALTER TABLE users DROP COLUMN IF EXISTS age;", drop.generateDropColumn("users", "age"));
        assertEquals("DROP INDEX IF EXISTS idx_users_email;", drop.generateDropIndex("users", "email"));
        assertEquals("DROP TABLE IF EXISTS users;", drop.generateDropTable("users"));
        assertEquals("ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_email_tenant_id;",
                drop.generateDropUniqueConstraint("users", "email", "tenant_id"));
        assertEquals("ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_role_id;",
                drop.generateDropForeignKey("users", "role_id"));

        assertEquals("ALTER TABLE users ALTER COLUMN age TYPE BIGINT;",
                modify.generateAlterColumnSetType("users", "age", "BIGINT"));
        assertEquals("ALTER TABLE users ALTER COLUMN age SET NOT NULL;",
                modify.generateAlterColumnSetNotNull("users", "age"));
        assertEquals("ALTER TABLE users ALTER COLUMN age DROP NOT NULL;",
                modify.generateAlterColumnDropNotNull("users", "age"));
        assertEquals("ALTER TABLE users ALTER COLUMN active SET DEFAULT false;",
                modify.generateAlterColumnSetDefault("users", "active", false));
        assertEquals("ALTER TABLE users ALTER COLUMN active DROP DEFAULT;",
                modify.generateAlterColumnDropDefault("users", "active"));
        assertEquals("ALTER TABLE old_users RENAME TO users;",
                modify.generateRenameTable("old_users", "users"));
        assertEquals("ALTER INDEX old_idx RENAME TO new_idx;",
                modify.generateRenameIndex("users", "old_idx", "new_idx"));
        assertEquals("ALTER TABLE users RENAME CONSTRAINT old_fk TO new_fk;",
                modify.generateRenameForeignKey("users", "old_fk", "new_fk"));
        assertEquals("ALTER TABLE users RENAME CONSTRAINT old_uq TO new_uq;",
                modify.generateRenameUniqueConstraint("users", "old_uq", "new_uq"));
        assertEquals("ALTER TABLE users RENAME COLUMN fname TO first_name;",
                modify.generateRenameColumn("users", "fname", "first_name"));

        assertThrows(Sdp4jValidationException.class, () -> drop.generateDropTable("bad-name"));
        assertThrows(Sdp4jValidationException.class, () -> modify.generateRenameColumn("users", "old", "new-name"));
    }

    @Test
    void sm4jBuildsMigrationStepMetadataForExplicitOperations() {
        SM4J sm4j = new SM4J(JdbcProxySupport.recordingDataSource(), "com.sdp4j.sm4j");

        assertStep(sm4j.dropColumn("users", "age"),
                "dropColumn(users, age)", "ALTER TABLE users DROP COLUMN IF EXISTS age;");
        assertStep(sm4j.dropIndex("users", "email"),
                "dropIndex(users, email)", "DROP INDEX IF EXISTS idx_users_email;");
        assertStep(sm4j.dropTable("users"),
                "dropTable(users)", "DROP TABLE IF EXISTS users;");
        assertStep(sm4j.dropUniqueConstraint("users", "email", "tenant_id"),
                "dropUniqueConstraint(users, email, tenant_id)",
                "ALTER TABLE users DROP CONSTRAINT IF EXISTS uq_users_email_tenant_id;");
        assertStep(sm4j.dropForeignKey("users", "role_id"),
                "dropForeignKey(users, role_id)",
                "ALTER TABLE users DROP CONSTRAINT IF EXISTS fk_users_role_id;");
        assertStep(sm4j.alterColumnSetType("users", "age", "BIGINT"),
                "alterColumnSetType(users, age, BIGINT)",
                "ALTER TABLE users ALTER COLUMN age TYPE BIGINT;");
        assertStep(sm4j.alterColumnSetDefault("users", "active", false),
                "alterColumnSetDefault(users, active, false)",
                "ALTER TABLE users ALTER COLUMN active SET DEFAULT false;");
        assertStep(sm4j.alterColumnDropDefault("users", "active"),
                "alterColumnDropDefault(users, active)",
                "ALTER TABLE users ALTER COLUMN active DROP DEFAULT;");
        assertStep(sm4j.alterColumnSetNotNull("users", "email"),
                "alterColumnSetNotNull(users, email)",
                "ALTER TABLE users ALTER COLUMN email SET NOT NULL;");
        assertStep(sm4j.alterColumnDropNotNull("users", "email"),
                "alterColumnDropNotNull(users, email)",
                "ALTER TABLE users ALTER COLUMN email DROP NOT NULL;");
        assertStep(sm4j.renameTable("old_users", "users"),
                "renameTable(old_users, users)",
                "ALTER TABLE old_users RENAME TO users;");
        assertStep(sm4j.renameColumn("users", "fname", "first_name"),
                "renameColumn(users, fname, first_name)",
                "ALTER TABLE users RENAME COLUMN fname TO first_name;");
    }

    @Test
    void migrationRecordsDefensivelyCopyLists() {
        MigrationStepMetadata step = new MigrationStepMetadata("verb", List.of("a", "b"), "DDL");
        MigrationMetadata migration = new MigrationMetadata(List.of(step));

        assertEquals("verb(a, b)", step.getAction());
        assertThrows(UnsupportedOperationException.class, () -> step.args().add("c"));
        assertThrows(UnsupportedOperationException.class, () -> migration.steps().add(step));
        assertFalse(migration.steps().isEmpty());
    }

    private static ColumnMetadata column(String name, String type) {
        ColumnMetadata column = new ColumnMetadata();
        column.setName(name);
        column.setType(type);
        return column;
    }

    private static void assertStep(MigrationStepMetadata step, String action, String ddl) {
        assertEquals(action, step.getAction());
        assertEquals(ddl, step.ddl());
    }
}

package com.sdp4j.simplemigration.services;

import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.simplemigration.annotations.Table;
import com.sdp4j.simplemigration.metadata.MigrationMetadata;
import com.sdp4j.simplemigration.metadata.TableMetadata;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Migration {

    private final DataSource dataSource;
    private final String packageName;
    private final MetadataParser metadataParser = new MetadataParser();
    private final DdlGenerator ddlGenerator = new DdlGenerator();

    public Migration(DataSource dataSource, String packageName) {
        this.dataSource = dataSource;
        this.packageName = packageName;
    }

    public void migrateSchema() throws SQLException {
        if (!CommonUtil.isValidString(packageName)) {
            throw new IllegalArgumentException("Package name must be provided for migration");
        }
        List<String> ddls = new ArrayList<>();
        for (Class<?> clazz : getClassesToMigrate()) {
            TableMetadata tableMetadata = metadataParser.parse(clazz);
            ddls.add(ddlGenerator.generateCreateTable(tableMetadata));
            ddls.add(ddlGenerator.generateForeignKeys(tableMetadata));
            ddls.add(ddlGenerator.generateIndexes(tableMetadata));
        }
        executeDdlsAndRecordMigration(ddls, "schema_migration");
    }

    public void migrateChanges(List<MigrationMetadata> changes) throws SQLException {
        List<String> executedMigrationsNames = getSortedMigrationsNames();
        List<MigrationMetadata> migrationsToExecute = changes.stream().filter(c -> !executedMigrationsNames.contains(c.getName())).toList();
        executeMigrations(migrationsToExecute);
    }

    public MigrationMetadata dropColumn(String columnName, String tableName) throws SQLException, IllegalArgumentException {
        if (!CommonUtil.areValidSqlIdentifiers(columnName, tableName)) {
            throw new IllegalArgumentException("Invalid identifier provided for delete column");
        }
        return new MigrationMetadata("dropColumn(" + columnName + ", " + tableName + ")",
                List.of(DROP_COLUMN_DDL
                        .replace(":table_name", tableName)
                        .replace(":column_name", columnName)));
    }

    public MigrationMetadata renameColumn(String oldColumnName, String newColumnName, String tableName) throws SQLException, IllegalArgumentException {
        List<String> ddls = new ArrayList<>();
        if (indexExistsByName(generateIndexName(tableName, oldColumnName))) {
            ddls.add(generateRenameIndexDdl(generateIndexName(tableName, oldColumnName), generateIndexName(tableName, newColumnName)));
        }
        if (foreignKeyExistsByName(generateForeignKeyName(tableName, oldColumnName))) {
            ddls.add(generateRenameForeignKeyDdl(tableName, generateForeignKeyName(tableName, oldColumnName), generateForeignKeyName(tableName, newColumnName)));
        }
        List<String> tableOldUniqueConstraints = getTableUniqueConstraints(tableName, oldColumnName);
        if (CommonUtil.isValidCollection(tableOldUniqueConstraints)) {
            List<String> newTableUniqueConstraints = tableOldUniqueConstraints.stream()
                    .map(c -> c.replace(oldColumnName, newColumnName))
                    .toList();
            for (int i = 0; i < tableOldUniqueConstraints.size(); i++) {
                ddls.add(RENAME_UNIQUE_CONSTRAINT_DDL
                        .replace(":table_name", tableName)
                        .replace(":old_unique_name", tableOldUniqueConstraints.get(i))
                        .replace(":new_unique_name", newTableUniqueConstraints.get(i)));
            }
            ddls.add(generateRenameColumnDdl(oldColumnName, newColumnName, tableName));
        }
        return new MigrationMetadata("renameColumn(" + oldColumnName + ", " + newColumnName + ", " + tableName + ")", ddls);
    }

    public MigrationMetadata dropIndex(String columnName, String tableName) {
        validateSqlIdentifiers(columnName, tableName);
        String indexName = generateIndexName(tableName, columnName);
        return new MigrationMetadata("dropIndex(" + columnName + ", " + tableName + ")", List.of(generateDropIndexDdl(indexName)));
    }

    public MigrationMetadata dropTable(String tableName) {
        validateSqlIdentifiers(tableName);
        return new MigrationMetadata("dropTable(" + tableName + ")",
                List.of(DROP_TABLE_DDL.replace(":table_name", tableName)));
    }

    public MigrationMetadata renameTable(String oldTableName, String newTableName) {
        validateSqlIdentifiers(oldTableName, newTableName);
        return new MigrationMetadata("renameTable(" + oldTableName + ", " + newTableName + ")",
                List.of(RENAME_TABLE_DDL
                        .replace(":old_table_name", oldTableName)
                        .replace(":new_table_name", newTableName)));
    }

    public MigrationMetadata dropUniqueConstraint(String tableName, String... columnNames) {
        validateSqlIdentifiers(tableName);
        validateSqlIdentifiers(columnNames);
        String constraintName = "unique_" + tableName + "_" + String.join("_", columnNames);
        return new MigrationMetadata("dropUniqueConstraint(" + tableName + ", " + String.join(", ", columnNames) + ")",
                List.of(DROP_CONSTRAINT_DDL
                        .replace(":table_name", tableName)
                        .replace(":constraint_name", constraintName)));
    }

    public MigrationMetadata dropForeignKey(String tableName, String... columnNames) {
        validateSqlIdentifiers(tableName);
        validateSqlIdentifiers(columnNames);
        String foreignKeyName = generateForeignKeyName(tableName, columnNames);
        return new MigrationMetadata("dropForeignKey(" + tableName + ", " + String.join(", ", columnNames) + ")",
                List.of(DROP_CONSTRAINT_DDL
                        .replace(":table_name", tableName)
                        .replace(":constraint_name", foreignKeyName)));
    }


    private List<String> getSortedMigrationsNames() throws SQLException {
        List<String> migrationNames = new ArrayList<>();
        ResultSet rs = null;
        Statement st = null;
        Connection connection = dataSource.getConnection();
        try {
            st = connection.createStatement();
            rs = st.executeQuery(GET_SORTED_MIGRATIONS);
            while (rs.next()) {
                migrationNames.add(rs.getString("script_name"));
            }
        } finally {
            CommonUtil.close(rs, st, connection);
        }
        return migrationNames;
    }

    private void executeMigrations(List<MigrationMetadata> migrations) throws SQLException {
        if (!CommonUtil.isValidCollection(migrations)) {
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement migrationPs = connection.prepareStatement(Sql.INSERT_MIGRATION);
                 Statement ddlSt = connection.createStatement()) {
                try {
                    for (MigrationMetadata migration : migrations) {
                        for (String script : migration.getScripts()) {
                            ddlSt.addBatch(script);
                        }
                        migrationPs.setString(1, migration.getName());
                        migrationPs.setString(2, String.join("\n", migration.getScripts()));
                        migrationPs.addBatch();
                    }
                    ddlSt.executeBatch();
                    migrationPs.executeBatch();
                    connection.commit();
                } catch (SQLException e) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackEx) {
                        e.addSuppressed(rollbackEx);
                    }
                    throw e;
                }
            }
        }
    }

    private String generateRenameForeignKeyDdl(String tableName, String oldForeignKeyName, String newForeignKeyName) {
        return RENAME_FOREIGN_KEY_DDL
                .replace(":table_name", tableName)
                .replace(":old_fk_name", oldForeignKeyName)
                .replace(":new_fk_name", newForeignKeyName);
    }

    private String generateIndexName(String tableName, String columnName) {
        return "idx_" + tableName + "_" + columnName;
    }

    private String generateForeignKeyName(String tableName, String... columnNames) {
        return "fk_" + tableName + "_" + String.join("_", columnNames);
    }

    private List<String> getTableUniqueConstraints(String tableName, String columnName) throws SQLException {
        List<String> uniqueConstraintNames = new ArrayList<>();
        ResultSet rs = null;
        PreparedStatement ps = null;
        Connection connection = dataSource.getConnection();
        try {
            ps = connection.prepareStatement(GET_TABLE_UNIQUE_CONSTRAINTS);
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            rs = ps.executeQuery();
            while (rs.next()) {
                uniqueConstraintNames.add(rs.getString("conname"));
            }
        } finally {
            CommonUtil.close(rs, ps, connection);
        }
        return uniqueConstraintNames;
    }


    private String generateRenameColumnDdl(String oldColumnName, String newColumnName, String tableName) {
        if (!CommonUtil.areValidSqlIdentifiers(newColumnName, oldColumnName, tableName)) {
            throw new IllegalArgumentException("Invalid identifier provided for rename column");
        }
        return RENAME_COLUMN_DDL
                .replace(":table_name", tableName)
                .replace(":old_column_name", oldColumnName)
                .replace(":new_column_name", newColumnName);
    }

    private boolean indexExistsByName(String indexName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(GET_INDEX_BY_NAME);) {
            ps.setString(1, CommonUtil.getOrDefault(connection.getSchema(), "public"));
            ps.setString(2, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean migrationExists(Connection connection, String migrationName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(Sql.CHECK_MIGRATION_EXISTS)) {
            ps.setString(1, migrationName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean foreignKeyExistsByName(String foreignKeyName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(CHECK_IF_FOREIGN_KEY_EXISTS_BY_NAME)) {
            ps.setString(1, "f");
            ps.setString(2, foreignKeyName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private String generateDropIndexDdl(String indexName) {
        return DROP_INDEX_DDL.replace(":index_name", indexName);
    }

    private String generateDropForeignKeyDdl(String tableName, String foreignKeyName) {
        return DROP_CONSTRAINT_DDL
                .replace(":table_name", tableName)
                .replace(":constraint_name", foreignKeyName);
    }

    private String generateRenameIndexDdl(String oldIndexName, String newIndexName) {
        return RENAME_INDEX_DDL
                .replace(":old_index_name", oldIndexName)
                .replace(":new_index_name", newIndexName);
    }

    private List<Class<?>> getClassesToMigrate() {
        try (ScanResult scan = new ClassGraph()
                .acceptPackages(packageName)
                .enableAnnotationInfo()
                .scan()) {
            return scan.getClassesWithAnnotation(Table.class.getName()).loadClasses();
        }
    }

    private void ensureMigrationsTableExists(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(Sql.CREATE_MIGRATIONS_TABLE);
        }
    }

    private void recordMigration(Connection connection, String scriptName, List<String> ddls) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(Sql.INSERT_MIGRATION)) {
            ps.setString(1, scriptName);
            ps.setString(2, String.join("\n", ddls));
            ps.execute();
        }
    }

    private void executeDdls(Connection connection, List<String> ddls) throws SQLException {
        if (!CommonUtil.isValidCollection(ddls)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            for (String ddl : ddls) {
                if (CommonUtil.isValidString(ddl)) {
                    statement.addBatch(ddl);
                }
            }
            statement.executeBatch();
        }
    }

    private void executeDdlsAndRecordMigration(List<String> ddls, String migrationName) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (migrationExists(connection, migrationName)) {
                    connection.commit();
                    return;
                }
                executeDdls(connection, ddls);
                recordMigration(connection, migrationName, ddls);
                connection.commit();
            } catch (SQLException e) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            }
        }
    }

    private void validateSqlIdentifiers(String... identifiers) {
        if (!CommonUtil.areValidSqlIdentifiers(identifiers)) {
            throw new IllegalArgumentException("Invalid identifier provided for delete column");
        }
    }

    static class Sql {

        private static final String CREATE_MIGRATIONS_TABLE = """
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    id UUID PRIMARY KEY,
                    script_name VARCHAR(255) NOT NULL,
                    script TEXT,
                    migrated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """;

        private static final String INSERT_MIGRATION =
                "INSERT INTO schema_migrations (id, script_name, script, migrated_at) VALUES (gen_random_uuid(), ?, ?, NOW())";

        private static final String CHECK_MIGRATION_EXISTS =
                "SELECT 1 FROM schema_migrations WHERE script_name = ?";
    }

    public static final String RENAME_COLUMN_DDL = """
            ALTER TABLE :table_name
                rename column :old_column_name to :new_column_name;
            """;

    public static final String GET_INDEX_BY_NAME = """
            SELECT indexname
            FROM pg_indexes
            WHERE schemaname = ?
              AND indexname = ?;
            """;

    public static final String RENAME_INDEX_DDL = """
            ALTER INDEX :old_index_name
            RENAME TO :new_index_name;
            """;

    public static final String CHECK_IF_FOREIGN_KEY_EXISTS_BY_NAME = """
                SELECT 1
                FROM pg_constraint
                WHERE contype = ?
                  AND conname = ?;
            """;

    public static final String RENAME_FOREIGN_KEY_DDL = """
            ALTER TABLE :table_name
            RENAME CONSTRAINT :old_fk_name TO :new_fk_name;
            """;

    public static final String GET_TABLE_UNIQUE_CONSTRAINTS = """
            SELECT c.conname
            FROM pg_constraint c
            JOIN pg_class t ON c.conrelid = t.oid
            JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
            WHERE t.relname = ?
              AND c.contype = 'u'
              AND a.attname = ?
            """;

    public static final String RENAME_UNIQUE_CONSTRAINT_DDL = """
            ALTER TABLE :table_name
            RENAME CONSTRAINT :old_unique_name TO :new_unique_name;
            """;

    public static final String GET_SORTED_MIGRATIONS = """
            SELECT script_name
            FROM schema_migrations
            ORDER BY migrated_at ASC;
            """;

    public static final String DROP_COLUMN_DDL = """
            ALTER TABLE :table_name
                DROP COLUMN :column_name;
            """;

    public static final String DROP_INDEX_DDL = """
            DROP INDEX IF EXISTS :index_name;
            """;

    public static final String DROP_CONSTRAINT_DDL = """
            ALTER TABLE :table_name
                DROP CONSTRAINT IF EXISTS :constraint_name;
            """;

    public static final String DROP_TABLE_DDL = """
            DROP TABLE IF EXISTS :table_name;
            """;

    public static final String RENAME_TABLE_DDL = """
            ALTER TABLE :old_table_name
                RENAME TO :new_table_name;
            """;
}

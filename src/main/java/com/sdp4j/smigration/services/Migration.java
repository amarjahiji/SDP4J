package com.sdp4j.smigration.services;

import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.smigration.annotations.Table;
import com.sdp4j.smigration.metadata.TableMetadata;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Migration {

    private final DataSource dataSource;
    private final String packageName;
    private final MetadataParser metadataParser = new MetadataParser();
    private final DdlGenerator ddlGenerator = new DdlGenerator();

    /**
     * Create a Migration configured with the JDBC data source and base package to scan for entity classes.
     *
     * @param dataSource  JDBC DataSource used to obtain connections for executing DDL statements
     * @param packageName base package to scan for classes annotated with `@Table`
     */
    public Migration(DataSource dataSource, String packageName) {
        this.dataSource = dataSource;
        this.packageName = packageName;
    }

    /**
     * Applies schema migrations discovered in the configured package to the target database and writes the generated SQL to a migration file.
     *
     * This method scans the configured package for classes annotated with @Table, generates CREATE TABLE, foreign key, and index statements,
     * executes those statements inside a single transaction (ensuring a schema_migrations table exists and recording the migration), and then
     * writes the produced SQL statements to a new file under the migrations directory.
     *
     * @throws IllegalArgumentException if the configured package name is null or empty
     * @throws SQLException if a database operation fails (including execution of DDL, recording the migration, or commit/rollback failures)
     * @throws IOException if writing the migration file to disk fails
     */
    public void migrateSchema() throws SQLException, IOException {
        if (!CommonUtil.isValidString(packageName)) {
            throw new IllegalArgumentException("Package name must be provided for migration");
        }
        List<String> createTablesStatements = new ArrayList<>();
        List<String> createForeignKeyStatements = new ArrayList<>();
        List<String> createIndexStatements = new ArrayList<>();

        for (Class<?> clazz : getClassesToMigrate()) {
            TableMetadata tableMetadata = metadataParser.parse(clazz);
            createTablesStatements.add(ddlGenerator.generateCreateTable(tableMetadata));
            createForeignKeyStatements.add(ddlGenerator.generateForeignKeys(tableMetadata));
            createIndexStatements.add(ddlGenerator.generateIndexes(tableMetadata));
        }

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ensureMigrationsTableExists(connection);
                executeDdls(connection, createTablesStatements);
                executeDdls(connection, createForeignKeyStatements);
                executeDdls(connection, createIndexStatements);
                recordMigration(connection, "migrate_schema");
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
        writeMigrationFile("migrateSchema" + LocalDateTime.now(), createTablesStatements, createForeignKeyStatements, createIndexStatements);
    }

    /**
     * Scans the configured base package and collects classes annotated with {@code @Table}.
     *
     * @return a list of classes annotated with {@code @Table} found under the configured package; an empty list if none are found
     */
    private List<Class<?>> getClassesToMigrate() {
        try (ScanResult scan = new ClassGraph()
                .acceptPackages(packageName)
                .enableAnnotationInfo()
                .scan()) {
            return scan.getClassesWithAnnotation(Table.class.getName()).loadClasses();
        }
    }

    /**
     * Ensures the schema_migrations table exists in the database.
     *
     * Executes the DDL defined by {@code Sql.CREATE_MIGRATIONS_TABLE}, creating the
     * migrations table if it does not already exist.
     *
     * @param connection the JDBC connection used to execute the statement
     * @throws SQLException if creating or executing the statement fails
     */
    private void ensureMigrationsTableExists(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(Sql.CREATE_MIGRATIONS_TABLE);
        }
    }

    /**
     * Inserts a migration record into the `schema_migrations` table for the given script name.
     *
     * The record stores `script_name` as the provided value and `migrated_at` as the current timestamp.
     *
     * @param scriptName the name of the migration script to record in the migrations table
     * @throws SQLException if the database insert fails
     */
    private void recordMigration(Connection connection, String scriptName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(Sql.INSERT_MIGRATION)) {
            ps.setString(1, scriptName);
            ps.execute();
        }
    }

    /**
     * Executes the given DDL statements as a JDBC batch on the provided connection.
     *
     * Invalid or blank statements are ignored; if the collection is null or empty, no action is taken.
     *
     * @param ddls the list of DDL SQL strings to execute; blank or invalid entries will be skipped
     * @throws SQLException if adding statements to the batch or executing the batch fails
     */
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

    /**
     * Writes the provided SQL statements to a new migration file under the local `migrations` directory.
     *
     * The file is created at `migrations/{scriptName}` and will contain create-table statements first,
     * followed by foreign-key statements, then index statements; each statement is separated by a blank line.
     *
     * @param scriptName   name to use for the migration file (used as the filename under `migrations`)
     * @param createTables ordered list of CREATE TABLE SQL statements to include first
     * @param foreignKeys  ordered list of FOREIGN KEY SQL statements to include after create-tables
     * @param indexes      ordered list of INDEX SQL statements to include last
     * @throws IOException if directory creation or file writing fails, or if the target file already exists
     */
    private void writeMigrationFile(String scriptName, List<String> createTables, List<String> foreignKeys, List<String> indexes) throws IOException {
        Path dir = Paths.get("migrations");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Path file = dir.resolve(scriptName);
        StringBuilder content = new StringBuilder();
        for (String sql : createTables) {
            if (CommonUtil.isValidString(sql)) {
                content.append(sql).append("\n\n");
            }
        }
        for (String sql : foreignKeys) {
            if (CommonUtil.isValidString(sql)) {
                content.append(sql).append("\n\n");
            }
        }
        for (String sql : indexes) {
            if (CommonUtil.isValidString(sql)) {
                content.append(sql).append("\n\n");
            }
        }
        Files.writeString(file, content.toString().strip(), StandardOpenOption.CREATE_NEW);
    }

    static class Sql {

        private static final String CREATE_MIGRATIONS_TABLE = """
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    id BIGSERIAL PRIMARY KEY,
                    script_name VARCHAR(255) NOT NULL,
                    migrated_at TIMESTAMP NOT NULL DEFAULT NOW()
                )
                """;

        private static final String INSERT_MIGRATION =
                "INSERT INTO schema_migrations (script_name, migrated_at) VALUES (?, NOW())";
    }
}

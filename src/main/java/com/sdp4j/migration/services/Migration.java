package com.sdp4j.migration.services;

import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.migration.annotations.Table;
import com.sdp4j.migration.metadata.TableMetadata;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Migration {

    private DataSource dataSource;
    private String packageName;

    /**
     * Private no-argument constructor to prevent creating a Migration instance without required dependencies.
     */
    private Migration() {
    }

    /**
     * Creates a Migration configured with the JDBC DataSource and the package to scan for migration entities.
     *
     * @param packageName the Java package to scan for classes annotated with `@Table`
     */
    public Migration(DataSource dataSource, String packageName) {
        this.dataSource = dataSource;
        this.packageName = packageName;
    }

    private final MetadataParser metadataParser = new MetadataParser();
    private final DdlGenerator ddlGenerator = new DdlGenerator();

    /**
     * Generates DDL for table, foreign key, and index statements from classes annotated with `@Table` in the configured package, executes them transactionally against the configured `DataSource`, and writes the resulting migration SQL to a timestamped file.
     *
     * @throws SQLException if executing the generated DDLs, committing the transaction, or rolling back on error fails
     * @throws IOException if writing the migration file fails
     */
    public void migrateSchema() throws SQLException, IOException {
        if (!CommonUtil.isValidString(packageName)) {
            throw new IllegalArgumentException("Package name  must be provided in MigrationClient for migration");
        }
        List<String> createTablesStatements = new ArrayList<>();
        List<String> createForeignKeyStatements = new ArrayList<>();
        List<String> createIndexStatements = new ArrayList<>();
        List<Class<?>> classesToMigrate = getClassesToMigrate(packageName);

        for (Class<?> clazz : classesToMigrate) {
            TableMetadata tableMetadata = metadataParser.parse(clazz);
            createTablesStatements.add(ddlGenerator.generateCreateTable(tableMetadata));
            createForeignKeyStatements.add(ddlGenerator.generateForeignKeys(tableMetadata));
            createIndexStatements.add(ddlGenerator.generateIndexes(tableMetadata));
        }
        executeDdls(dataSource, createTablesStatements, createForeignKeyStatements, createIndexStatements);
        writeMigrationFile(createTablesStatements, createForeignKeyStatements, createIndexStatements);
    }

    /**
     * Finds all classes within the given Java package that are annotated with `@Table`.
     *
     * @param packageName the Java package to scan for classes annotated with `@Table`
     * @return a list of classes in the specified package that are annotated with `@Table`
     */
    public List<Class<?>> getClassesToMigrate(String packageName) {
        try (ScanResult scan = new ClassGraph()
                .acceptPackages(packageName)
                .enableAnnotationInfo()
                .scan()) {
            return scan.getClassesWithAnnotation(Table.class.getName())
                    .loadClasses();
        }
    }

    /**
     * Execute the provided DDL statements on the given JDBC connection using a batch.
     *
     * If `ddls` is null or empty, this method returns without performing any work.
     *
     * @param connection the JDBC Connection to create the statement on and execute the batch
     * @param ddls      the list of SQL DDL statements to execute; null or blank entries are ignored
     * @throws SQLException if creating the statement, adding or executing the batch, or closing the statement fails
     */
    private void executeDdls(Connection connection, List<String> ddls) throws SQLException {
        if (ddls == null || ddls.isEmpty()) {
            return;
        }
        Statement statement = null;
        try {
            statement = connection.createStatement();
            for (String ddl : ddls) {
                if (ddl == null || ddl.isBlank()) {
                    continue;
                }
                statement.addBatch(ddl);
            }
            statement.executeBatch();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    /**
     * Executes the supplied DDL statements in order (tables, foreign keys, indexes) using a single JDBC connection and commits the transaction on success.
     *
     * @param createTablesDdls       list of CREATE TABLE SQL statements to execute (may contain null/blank entries which will be skipped)
     * @param createForeignKeysDdls  list of ALTER TABLE / FOREIGN KEY SQL statements to execute (may contain null/blank entries which will be skipped)
     * @param createIndexDdls        list of CREATE INDEX SQL statements to execute (may contain null/blank entries which will be skipped)
     * @throws SQLException if executing the DDL statements or committing the transaction fails; a rollback is attempted and any rollback failure is added as a suppressed exception
     */
    private void executeDdls(DataSource dataSource, List<String> createTablesDdls, List<String> createForeignKeysDdls, List<String> createIndexDdls) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(false);
            executeDdls(connection, createTablesDdls);
            executeDdls(connection, createForeignKeysDdls);
            executeDdls(connection, createIndexDdls);
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            throw e;
        } finally {
            connection.close();
        }
    }

    /**
     * Writes the provided DDL statements to a new timestamped SQL file under the `migrations` directory,
     * grouping statements in the order: create tables, foreign keys, then indexes.
     *
     * Each list entry that is non-null and not blank is written as a separate statement block; the method
     * ensures the `migrations` directory exists and creates a file named `migration_yyyy-MM-dd_HH-mm-ss.sql`.
     *
     * @param createTables  list of CREATE TABLE statements to include (may contain null or blank entries)
     * @param foreignKeys   list of CREATE FOREIGN KEY statements to include (may contain null or blank entries)
     * @param indexes       list of CREATE INDEX statements to include (may contain null or blank entries)
     * @throws IOException if creating the directory or file, or writing the file fails
     */
    private void writeMigrationFile(List<String> createTables, List<String> foreignKeys, List<String> indexes) throws IOException {
        Path dir = Paths.get("migrations");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        Path file = dir.resolve("migration_" + timestamp + ".sql");
        StringBuilder content = new StringBuilder();
        content.append("\n\n");
        for (String sql : createTables) {
            if (sql != null && !sql.isBlank()) {
                content.append(sql).append("\n\n");
            }
        }
        content.append("\n\n");
        for (String sql : foreignKeys) {
            if (sql != null && !sql.isBlank()) {
                content.append(sql).append("\n\n");
            }
        }
        content.append("\n\n");
        for (String sql : indexes) {
            if (sql != null && !sql.isBlank()) {
                content.append(sql).append("\n\n");
            }
        }
        Files.writeString(file, content, StandardOpenOption.CREATE_NEW);
    }
}

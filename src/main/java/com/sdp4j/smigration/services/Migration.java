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

    public Migration(DataSource dataSource, String packageName) {
        this.dataSource = dataSource;
        this.packageName = packageName;
    }

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

    private void recordMigration(Connection connection, String scriptName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(Sql.INSERT_MIGRATION)) {
            ps.setString(1, scriptName);
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

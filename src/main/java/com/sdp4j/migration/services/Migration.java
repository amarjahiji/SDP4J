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

    private Migration() {
    }

    public Migration(DataSource dataSource, String packageName) {
        this.dataSource = dataSource;
        this.packageName = packageName;
    }

    private final MetadataParser metadataParser = new MetadataParser();
    private final DdlGenerator ddlGenerator = new DdlGenerator();

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

    public List<Class<?>> getClassesToMigrate(String packageName) {
        try (ScanResult scan = new ClassGraph()
                .acceptPackages(packageName)
                .enableAnnotationInfo()
                .scan()) {
            return scan.getClassesWithAnnotation(Table.class.getName())
                    .loadClasses();
        }
    }

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

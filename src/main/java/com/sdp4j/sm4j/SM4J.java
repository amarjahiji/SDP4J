package com.sdp4j.sm4j;

import com.sdp4j.core.exception.Sm4jException;
import com.sdp4j.core.exception.Sdp4jQueryException;
import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.sm4j.annotations.Table;
import com.sdp4j.sm4j.ddlgenerators.*;
import com.sdp4j.sm4j.metadata.*;
import com.sdp4j.sm4j.parsers.MetadataParser;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class SM4J {

    private final String packageName;
    private final DataSource dataSource;

    private final MetadataParser metadataParser = new MetadataParser();
    private final CommonDdlsGenerator ddlGenerator = new CommonDdlsGenerator();
    private final CreateDdlsGenerator createDdlGenerator = new CreateDdlsGenerator();
    private final AddDdlsGenerator addDdlGenerator = new AddDdlsGenerator();
    private final ModifyDdlsGenerator modifyDdlsGenerator = new ModifyDdlsGenerator();
    private final DropDdlsGenerator dropDdlsGenerator = new DropDdlsGenerator();

    public SM4J(DataSource dataSource, String packageName) {
        if (!CommonUtil.isValidString(packageName)) {
            throw new Sdp4jValidationException("Package name must be provided for migration");
        }
        this.dataSource = dataSource;
        this.packageName = packageName;
    }

    public void executeMigration(List<MigrationStepMetadata> dropAndOrModifySteps) {
        List<Class<?>> tableAnnotatedClasses = getTableAnnotatedClasses();
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                acquireMigrationLock(connection);
                planAndApplyMigration(connection, tableAnnotatedClasses, dropAndOrModifySteps);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(connection, e);
                throw (e instanceof Sm4jException sm) ? sm : new Sm4jException("Failed to execute migration", e);
            }
        } catch (SQLException e) {
            throw new Sm4jException("Failed to execute migration", e);
        }
    }

    private void rollbackQuietly(Connection connection, Throwable primary) {
        try {
            connection.rollback();
        } catch (SQLException rollbackEx) {
            primary.addSuppressed(rollbackEx);
        }
    }

    private void planAndApplyMigration(Connection connection, List<Class<?>> tableAnnotatedClasses,
                                       List<MigrationStepMetadata> dropAndOrModifySteps) {
        List<String> existingDbTablesNames = getExistingDbTablesNames(connection);
        if (!existsMigrationInDb(existingDbTablesNames)) {
            applyInitialSchemaMigration(connection, tableAnnotatedClasses);
            return;
        }
        List<MigrationStepMetadata> dropAndOrModifyStepsToApply = filterOutAlreadyAppliedSteps(connection, dropAndOrModifySteps);
        PendingDeleteModifyStepsMetadata pendingDeleteModifyStepsMetadata = parsePendingDeleteModifyExistingSchemaElementsMetadata(dropAndOrModifyStepsToApply);
        Set<String> tablesToInspect = findTablesToInspect(tableAnnotatedClasses, existingDbTablesNames, pendingDeleteModifyStepsMetadata);
        Map<String, List<String>> columnsByTable = getColumnNamesGroupedByTablesNames(connection, tablesToInspect);
        PendingAdditiveStepsMetadata pendingAdditiveStepsMetadata = buildPendingAdditiveStepsMetadata(tableAnnotatedClasses, existingDbTablesNames, columnsByTable, pendingDeleteModifyStepsMetadata);

        List<MigrationStepMetadata> orderedStepsToApplyMetadata = new ArrayList<>();
        orderedStepsToApplyMetadata.addAll(pendingAdditiveStepsMetadata.getCreateTable());
        orderedStepsToApplyMetadata.addAll(pendingAdditiveStepsMetadata.getAddColumn());
        orderedStepsToApplyMetadata.addAll(dropAndOrModifyStepsToApply);
        orderedStepsToApplyMetadata.addAll(pendingAdditiveStepsMetadata.getAddForeignKey());
        orderedStepsToApplyMetadata.addAll(pendingAdditiveStepsMetadata.getAddIndex());
        applyMigrations(connection, new MigrationMetadata(orderedStepsToApplyMetadata));
    }

    private boolean existsMigrationInDb(List<String> existingDbTablesNames) {
        return existingDbTablesNames.contains("migrations") && existingDbTablesNames.contains("migration_steps");
    }

    private List<MigrationStepMetadata> filterOutAlreadyAppliedSteps(Connection connection, List<MigrationStepMetadata> dropModifyStepsMetadata) {
        Set<String> applied = new HashSet<>(getSortedAppliedStepActions(connection));
        return dropModifyStepsMetadata.stream()
                .filter(s -> !applied.contains(s.getAction()))
                .toList();
    }

    private Set<String> findTablesToInspect(List<Class<?>> tableAnnotatedClasses,
                                            List<String> existingDbTablesNames,
                                            PendingDeleteModifyStepsMetadata pending) {
        Set<String> tablesToInspect = new HashSet<>();
        for (Class<?> clazz : tableAnnotatedClasses) {
            String classTableName = metadataParser.parse(clazz).getName();
            if (pending.getDropTables().contains(classTableName)) continue;
            String currentDbName = effectiveDbName(classTableName, pending);
            if (existingDbTablesNames.contains(currentDbName)) {
                tablesToInspect.add(currentDbName);
            }
        }
        return tablesToInspect;
    }

    private PendingAdditiveStepsMetadata buildPendingAdditiveStepsMetadata(List<Class<?>> tableAnnotatedClasses,
                                                                           List<String> existingDbTablesNames,
                                                                           Map<String, List<String>> columnsByTable,
                                                                           PendingDeleteModifyStepsMetadata pending) {
        PendingAdditiveStepsMetadata pendingAdditiveStepsMetadata = new PendingAdditiveStepsMetadata();
        for (Class<?> clazz : tableAnnotatedClasses) {
            TableMetadata tableMetadata = metadataParser.parse(clazz);
            String classTableName = tableMetadata.getName();
            if (pending.getDropTables().contains(classTableName)) {
                continue;
            }
            boolean tableExistsInDb = existingDbTablesNames.contains(classTableName);
            boolean tableIsRenameTarget = pending.getRenameTables().containsValue(classTableName);
            if (!tableExistsInDb && !tableIsRenameTarget) {
                appendCreateTableSteps(pendingAdditiveStepsMetadata, classTableName, tableMetadata);
                continue;
            }
            String currentDbTableName = effectiveDbName(classTableName, pending);
            List<String> currentDbColumnNames = columnsByTable.getOrDefault(currentDbTableName, List.of());
            List<ColumnMetadata> newColumns = detectNewColumnsToBeAdded(tableMetadata, currentDbTableName, currentDbColumnNames, pending);
            appendAddColumnSteps(pendingAdditiveStepsMetadata, classTableName, currentDbTableName, newColumns);
        }
        return pendingAdditiveStepsMetadata;
    }

    private void appendCreateTableSteps(PendingAdditiveStepsMetadata pendingAdditiveStepsMetadata, String classTableName, TableMetadata tableMetadata) {
        pendingAdditiveStepsMetadata.getCreateTable().add(new MigrationStepMetadata("createTable",
                createDdlGenerator.generateTableWithoutFkAndIdx(tableMetadata),
                classTableName));
        for (ColumnMetadata column : tableMetadata.getColumnMetadata()) {
            addDdlGenerator.generateAddForeignKey(classTableName, column).ifPresent(d ->
                    pendingAdditiveStepsMetadata.getAddForeignKey().add(new MigrationStepMetadata(
                            "addForeignKey", d, classTableName, column.getName())));
            if (column.isIndex()) {
                pendingAdditiveStepsMetadata.getAddIndex().add(new MigrationStepMetadata("addIndex",
                        addDdlGenerator.generateAddIndex(classTableName, column.getName()),
                        classTableName, column.getName()));
            }
        }
    }

    private void appendAddColumnSteps(PendingAdditiveStepsMetadata pendingAdditiveStepsMetadata, String classTableName, String currentDbTableName,
                                      List<ColumnMetadata> newColumns) {
        for (ColumnMetadata col : newColumns) {
            validateAddingNotNullToExistingColumn(col, classTableName);
            pendingAdditiveStepsMetadata.getAddColumn().add(new MigrationStepMetadata(
                    "addColumn",
                    addDdlGenerator.generateAddColumn(currentDbTableName, col),
                    currentDbTableName, col.getName()));
            addDdlGenerator.generateAddForeignKey(classTableName, col).ifPresent(d ->
                    pendingAdditiveStepsMetadata.getAddForeignKey().add(new MigrationStepMetadata(
                            "addForeignKey", d, classTableName, col.getName())));
            if (col.isIndex()) {
                pendingAdditiveStepsMetadata.getAddIndex().add(new MigrationStepMetadata("addIndex",
                        addDdlGenerator.generateAddIndex(classTableName, col.getName()),
                        classTableName, col.getName()));
            }
        }
    }

    public MigrationStepMetadata dropColumn(String tableName, String columnName) {
        return new MigrationStepMetadata("dropColumn",
                dropDdlsGenerator.generateDropColumn(tableName, columnName),
                tableName, columnName);
    }

    public MigrationStepMetadata dropIndex(String tableName, String columnName) {
        return new MigrationStepMetadata("dropIndex",
                dropDdlsGenerator.generateDropIndex(tableName, columnName),
                tableName, columnName);
    }

    public MigrationStepMetadata dropTable(String tableName) {
        return new MigrationStepMetadata("dropTable",
                dropDdlsGenerator.generateDropTable(tableName),
                tableName);
    }

    public MigrationStepMetadata dropUniqueConstraint(String tableName, String... columnNames) {
        List<String> args = new ArrayList<>(columnNames.length + 1);
        args.add(tableName);
        args.addAll(Arrays.asList(columnNames));
        return new MigrationStepMetadata("dropUniqueConstraint", args,
                dropDdlsGenerator.generateDropUniqueConstraint(tableName, columnNames));
    }

    public MigrationStepMetadata dropForeignKey(String tableName, String columnName) {
        return new MigrationStepMetadata("dropForeignKey",
                dropDdlsGenerator.generateDropForeignKey(tableName, columnName),
                tableName, columnName);
    }

    public MigrationStepMetadata alterColumnSetType(String tableName, String columnName, String type) {
        return new MigrationStepMetadata("alterColumnSetType",
                modifyDdlsGenerator.generateAlterColumnSetType(tableName, columnName, type),
                tableName, columnName, type);
    }

    public MigrationStepMetadata alterColumnSetDefault(String tableName, String columnName, Object defaultValue) {
        return new MigrationStepMetadata("alterColumnSetDefault",
                modifyDdlsGenerator.generateAlterColumnSetDefault(tableName, columnName, defaultValue),
                tableName, columnName, String.valueOf(defaultValue));
    }

    public MigrationStepMetadata alterColumnDropDefault(String tableName, String columnName) {
        return new MigrationStepMetadata("alterColumnDropDefault",
                modifyDdlsGenerator.generateAlterColumnDropDefault(tableName, columnName),
                tableName, columnName);
    }

    public MigrationStepMetadata alterColumnSetNotNull(String tableName, String columnName) {
        return new MigrationStepMetadata("alterColumnSetNotNull",
                modifyDdlsGenerator.generateAlterColumnSetNotNull(tableName, columnName),
                tableName, columnName);
    }

    public MigrationStepMetadata alterColumnDropNotNull(String tableName, String columnName) {
        return new MigrationStepMetadata("alterColumnDropNotNull",
                modifyDdlsGenerator.generateAlterColumnDropNotNull(tableName, columnName),
                tableName, columnName);
    }

    public MigrationStepMetadata renameTable(String oldTableName, String newTableName) {
        return new MigrationStepMetadata("renameTable",
                modifyDdlsGenerator.generateRenameTable(oldTableName, newTableName),
                oldTableName, newTableName);
    }

    public List<MigrationStepMetadata> renameColumn(String tableName, String oldColumnName, String newColumnName, boolean renameItsConstraintsAndIndexes) {
        List<MigrationStepMetadata> steps = new ArrayList<>();
        steps.add(new MigrationStepMetadata("renameColumn",
                modifyDdlsGenerator.generateRenameColumn(tableName, oldColumnName, newColumnName),
                tableName, oldColumnName, newColumnName));
        if (renameItsConstraintsAndIndexes) {
            appendDependentRenames(steps, tableName, oldColumnName, newColumnName);
        }
        return steps;
    }

    public MigrationStepMetadata renameColumn(String tableName, String oldColumnName, String newColumnName) {
        return renameColumn(tableName, oldColumnName, newColumnName, false).getFirst();
    }

    private void appendDependentRenames(List<MigrationStepMetadata> steps, String tableName, String oldColumnName, String newColumnName) {
        try (Connection connection = dataSource.getConnection()) {
            String oldIndexName = ddlGenerator.buildIndexName(tableName, oldColumnName);
            String newIndexName = ddlGenerator.buildIndexName(tableName, newColumnName);
            if (indexExistsByName(connection, oldIndexName)) {
                steps.add(new MigrationStepMetadata("renameIndex",
                        modifyDdlsGenerator.generateRenameIndex(tableName, oldIndexName, newIndexName),
                        tableName, oldIndexName, newIndexName));
            }
            String oldForeignKeyName = ddlGenerator.buildForeignKeyName(tableName, oldColumnName);
            String newForeignKeyName = ddlGenerator.buildForeignKeyName(tableName, newColumnName);
            if (foreignKeyExistsByName(connection, oldForeignKeyName)) {
                steps.add(new MigrationStepMetadata("renameForeignKey",
                        modifyDdlsGenerator.generateRenameForeignKey(tableName, oldForeignKeyName, newForeignKeyName),
                        tableName, oldForeignKeyName, newForeignKeyName));
            }
            for (String oldUc : getTableUniqueConstraints(connection, tableName, oldColumnName)) {
                String newUc = oldUc.replace(oldColumnName, newColumnName);
                steps.add(new MigrationStepMetadata("renameUniqueConstraint",
                        modifyDdlsGenerator.generateRenameUniqueConstraint(tableName, oldUc, newUc),
                        tableName, oldUc, newUc));
            }
        } catch (SQLException e) {
            throw new Sm4jException("Failed to inspect constraints/indexes for renameColumn on '" + tableName + "." + oldColumnName + "'", e);
        }
    }

    private void applyInitialSchemaMigration(Connection connection, List<Class<?>> classesToMigrate) {
        List<String> ddls = new ArrayList<>();
        ddls.add(Sql.CREATE_MIGRATIONS_TABLE);
        ddls.add(Sql.CREATE_MIGRATION_STEPS_TABLE);
        ddls.add(Sql.CREATE_MIGRATION_STEPS_INDEX);
        for (Class<?> clazz : classesToMigrate) {
            TableMetadata tableMetadata = metadataParser.parse(clazz);
            ddls.add(createDdlGenerator.generateTableWithoutFkAndIdx(tableMetadata));
            ddls.addAll(addDdlGenerator.generateAddForeignKeys(tableMetadata));
            ddls.addAll(addDdlGenerator.generateIndicesDdls(tableMetadata));
        }
        try {
            executeDdls(connection, ddls);
            UUID migrationId = insertMigration(connection);
            insertMigrationStep(connection, migrationId);
        } catch (SQLException e) {
            throw new Sm4jException(describeBatchFailure("Failed to execute initial schema migration", e, ddls), e);
        }
    }

    private PendingDeleteModifyStepsMetadata parsePendingDeleteModifyExistingSchemaElementsMetadata(List<MigrationStepMetadata> userStepsToApply) {
        PendingDeleteModifyStepsMetadata metadata = new PendingDeleteModifyStepsMetadata();
        for (MigrationStepMetadata step : userStepsToApply) {
            List<String> args = step.args();
            switch (step.verb()) {
                case "renameColumn" -> {
                    if (args.size() >= 3) {
                        metadata.getRenameColumns()
                                .computeIfAbsent(args.get(0), _ -> new HashMap<>())
                                .put(args.get(1), args.get(2));
                    }
                }
                case "renameTable" -> {
                    if (args.size() >= 2) {
                        metadata.getRenameTables().put(args.get(0), args.get(1));
                    }
                }
                case "dropTable" -> {
                    if (!args.isEmpty()) {
                        metadata.getDropTables().add(args.getFirst());
                    }
                }
                case "dropColumn" -> {
                    if (args.size() >= 2) {
                        metadata.getDropColumns()
                                .computeIfAbsent(args.get(0), _ -> new HashSet<>())
                                .add(args.get(1));
                    }
                }
                default -> {
                }
            }
        }
        return metadata;
    }

    private List<ColumnMetadata> detectNewColumnsToBeAdded(TableMetadata tableMetadata, String currentDbTableName,
                                                           List<String> existingDbColumnsNames, PendingDeleteModifyStepsMetadata pending) {
        Set<String> existingDbColumnsNamesToNotBeDropped = new HashSet<>(existingDbColumnsNames);
        Set<String> columnNamesToBeDropped = new HashSet<>();
        columnNamesToBeDropped.addAll(pending.getDropColumns().getOrDefault(currentDbTableName, Set.of()));
        columnNamesToBeDropped.addAll(pending.getDropColumns().getOrDefault(tableMetadata.getName(), Set.of()));
        existingDbColumnsNamesToNotBeDropped.removeAll(columnNamesToBeDropped);
        Map<String, String> existingDbColumnsNamesToBeRenamed = new HashMap<>();
        existingDbColumnsNamesToBeRenamed.putAll(pending.getRenameColumns().getOrDefault(currentDbTableName, Map.of()));
        existingDbColumnsNamesToBeRenamed.putAll(pending.getRenameColumns().getOrDefault(tableMetadata.getName(), Map.of()));
        for (Map.Entry<String, String> columnToRename : existingDbColumnsNamesToBeRenamed.entrySet()) {
            existingDbColumnsNamesToNotBeDropped.remove(columnToRename.getKey());
            existingDbColumnsNamesToNotBeDropped.add(columnToRename.getValue());
        }
        return tableMetadata.getColumnMetadata().stream()
                .filter(col -> !existingDbColumnsNamesToNotBeDropped.contains(col.getName()))
                .toList();
    }

    private Map<String, List<String>> getColumnNamesGroupedByTablesNames(Connection connection, Collection<String> tableNames) {
        Map<String, List<String>> result = new HashMap<>();
        if (tableNames.isEmpty()) {
            return result;
        }
        try (PreparedStatement ps = connection.prepareStatement(Sql.GET_TABLES_COLUMNS)) {
            String schema = CommonUtil.getOrDefault(connection.getSchema(), "public");
            Array tablesArray = connection.createArrayOf("VARCHAR", tableNames.toArray());
            ps.setString(1, schema);
            ps.setArray(2, tablesArray);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString("table_name");
                    String columnName = rs.getString("column_name");
                    result.computeIfAbsent(tableName, _ -> new ArrayList<>()).add(columnName);
                }
            }
        } catch (SQLException e) {
            throw new Sm4jException("Failed to retrieve tables columns from database", e);
        }
        return result;
    }


    private List<String> getExistingDbTablesNames(Connection connection) {
        List<String> tablesNames = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(Sql.GET_TABLES_NAMES)) {
            ps.setString(1, CommonUtil.getOrDefault(connection.getSchema(), "public"));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tablesNames.add(rs.getString("tablename"));
                }
            }
        } catch (SQLException e) {
            throw new Sdp4jQueryException("Failed to retrieve existing database table names", e);
        }
        return tablesNames;
    }

    private List<String> getSortedAppliedStepActions(Connection connection) {
        List<String> actions = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(Sql.GET_SORTED_STEP_ACTIONS)) {
            while (rs.next()) {
                actions.add(rs.getString("action"));
            }
        } catch (SQLException e) {
            throw new Sdp4jQueryException("Failed to retrieve applied migration steps from database", e);
        }
        return actions;
    }

    private void applyMigrations(Connection connection, MigrationMetadata migration) {
        List<MigrationStepMetadata> steps = migration.steps();
        if (!CommonUtil.isValidCollection(steps)) {
            return;
        }
        for (MigrationStepMetadata step : steps) {
            if (!CommonUtil.isValidString(step.ddl())) {
                throw new Sdp4jValidationException("Migration step '" + step.getAction() + "' has an empty DDL");
            }
        }
        try (Statement ddlSt = connection.createStatement();
             PreparedStatement stepPs = connection.prepareStatement(Sql.INSERT_MIGRATION_STEP)) {
            UUID migrationId = insertMigration(connection);
            for (MigrationStepMetadata step : steps) {
                ddlSt.addBatch(step.ddl());
                stepPs.setObject(1, UUID.randomUUID());
                stepPs.setString(2, step.getAction());
                stepPs.setObject(3, migrationId);
                stepPs.addBatch();
            }
            ddlSt.executeBatch();
            stepPs.executeBatch();
        } catch (SQLException e) {
            throw new Sm4jException(describeBatchFailure("Failed to execute migrations", e, steps), e);
        }
    }

    private void acquireMigrationLock(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            long MIGRATION_LOCK_KEY = 7836413L;
            st.execute("SELECT pg_advisory_xact_lock(" + MIGRATION_LOCK_KEY + ")");
        }
    }

    private UUID insertMigration(Connection connection) throws SQLException {
        UUID id = UUID.randomUUID();
        try (PreparedStatement ps = connection.prepareStatement(Sql.INSERT_MIGRATION)) {
            ps.setObject(1, id);
            ps.execute();
        }
        return id;
    }

    private void insertMigrationStep(Connection connection, UUID migrationId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(Sql.INSERT_MIGRATION_STEP)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, "initial_schema_migration()");
            ps.setObject(3, migrationId);
            ps.execute();
        }
    }

    private List<String> getTableUniqueConstraints(Connection connection, String tableName, String columnName) throws SQLException {
        List<String> uniqueConstraintNames = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(Sql.GET_TABLE_UNIQUE_CONSTRAINTS)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    uniqueConstraintNames.add(rs.getString("conname"));
                }
            }
        }
        return uniqueConstraintNames;
    }

    private boolean indexExistsByName(Connection connection, String indexName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(Sql.GET_INDEX_BY_NAME)) {
            ps.setString(1, CommonUtil.getOrDefault(connection.getSchema(), "public"));
            ps.setString(2, indexName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean foreignKeyExistsByName(Connection connection, String foreignKeyName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(Sql.CHECK_IF_FOREIGN_KEY_EXISTS_BY_NAME)) {
            ps.setString(1, "f");
            ps.setString(2, foreignKeyName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<Class<?>> getTableAnnotatedClasses() {
        try (ScanResult scan = new ClassGraph()
                .acceptPackages(packageName)
                .enableAnnotationInfo()
                .scan()) {
            return scan.getClassesWithAnnotation(Table.class.getName()).loadClasses();
        } catch (RuntimeException e) {
            throw new Sm4jException(
                    "Failed to scan package '" + packageName + "' for @Table classes", e);
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

    private static String describeBatchFailure(String prefix, SQLException e, List<?> items) {
        if (!(e instanceof BatchUpdateException bue)) {
            return prefix;
        }
        int[] counts = bue.getUpdateCounts();
        for (int i = 0; i < counts.length && i < items.size(); i++) {
            if (counts[i] == Statement.EXECUTE_FAILED) {
                Object item = items.get(i);
                String desc = item instanceof MigrationStepMetadata step
                        ? step.getAction() + " -> " + step.ddl()
                        : String.valueOf(item);
                return prefix + " at step " + i + ": " + desc;
            }
        }
        return prefix;
    }

    private static boolean hasDefault(ColumnMetadata col) {
        return col.isDefaultTrue() || col.isDefaultFalse()
                || col.getDefaultIntValue() != null
                || col.getDefaultBigIntValue() != null
                || col.getDefaultRealValue() != null
                || col.getDefaultDoublePrecisionValue() != null
                || col.getDefaultNumericValue() != null
                || col.getDefaultStringValue() != null;
    }

    private static String effectiveDbName(String classTableName, PendingDeleteModifyStepsMetadata pending) {
        return pending.getRenameTables().entrySet().stream()
                .filter(e -> e.getValue().equals(classTableName))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(classTableName);
    }

    private static void validateAddingNotNullToExistingColumn(ColumnMetadata column, String classTableName) {
        if (!column.isNullable() && !hasDefault(column)) {
            throw new Sdp4jValidationException(
                    "Cannot auto-add NOT NULL column '" + column.getName() + "' to existing table '"
                            + classTableName + "' without a default. " +
                            "Add a @Default* annotation, remove @NotNull.");
        }
    }

    static class Sql {

        private static final String CREATE_MIGRATIONS_TABLE = """
                CREATE TABLE IF NOT EXISTS migrations (
                    id UUID PRIMARY KEY,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW()
                );
                """;

        private static final String CREATE_MIGRATION_STEPS_TABLE = """
                CREATE TABLE IF NOT EXISTS migration_steps (
                    id UUID PRIMARY KEY,
                    action TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    migration_id UUID NOT NULL REFERENCES migrations(id) ON DELETE CASCADE
                );
                """;

        private static final String CREATE_MIGRATION_STEPS_INDEX =
                "CREATE INDEX IF NOT EXISTS idx_migration_steps_migration_id ON migration_steps(migration_id);";

        private static final String INSERT_MIGRATION =
                "INSERT INTO migrations (id, created_at) VALUES (?, NOW())";

        private static final String INSERT_MIGRATION_STEP =
                "INSERT INTO migration_steps (id, action, created_at, migration_id) VALUES (?, ?, NOW(), ?)";

        public static final String GET_INDEX_BY_NAME = """
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = ?
                  AND indexname = ?;
                """;

        public static final String CHECK_IF_FOREIGN_KEY_EXISTS_BY_NAME = """
                    SELECT 1
                    FROM pg_constraint
                    WHERE contype = ?
                      AND conname = ?;
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

        public static final String GET_SORTED_STEP_ACTIONS = """
                SELECT ms.action
                FROM migration_steps ms
                JOIN migrations m ON m.id = ms.migration_id
                ORDER BY m.created_at ASC, ms.created_at ASC;
                """;

        public static final String GET_TABLES_NAMES = """
                SELECT tablename
                FROM pg_tables
                WHERE schemaname = ?;
                """;

        public static final String GET_TABLES_COLUMNS = """
                SELECT table_name, column_name
                FROM information_schema.columns
                WHERE table_schema = ?
                  AND table_name = ANY(?);
                """;
    }
}
package com.sdp4j.migration.services;


import com.sdp4j.migration.metadata.ColumnMetadata;
import com.sdp4j.migration.metadata.TableMetadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DdlGenerator {

    /**
     * Builds a CREATE TABLE SQL statement for the given table metadata.
     *
     * @param table metadata containing the table name, columns, primary key and unique-key constraints used to construct the statement
     * @return the CREATE TABLE statement with column definitions, primary key clause and any unique-key constraints
     */
    public String generateCreateTable(TableMetadata table) {
        String columnDdl = table.getColumnMetadata().stream()
                .map(this::generateColumn)
                .collect(Collectors.joining(",\n"));
        String primaryKeyDdl = generatePrimaryKey(table.getColumnMetadata(), table.getName());
        return "\nCREATE TABLE " + table.getName() + " (\n" +
                columnDdl +
                ",\n" + primaryKeyDdl +
                "\n" + generateUniqueKeysConstraints(table) +
                "\n);";
    }

    /**
     * Builds the SQL fragment that defines a single table column, including its name, type, nullability, and default value.
     *
     * @param column metadata describing the column to generate
     * @return the column definition SQL (for example: "id BIGINT NOT NULL DEFAULT 0")
     */
    private String generateColumn(ColumnMetadata column) {
        StringBuilder sql = new StringBuilder();
        sql.append(column.getName())
                .append(" ")
                .append(column.getType());
        if (!column.isNullable()) {
            sql.append(" NOT NULL");
        }
        String defaultValue = generateDefaultValue(column);
        if (defaultValue != null) {
            sql.append(defaultValue);
        }
        return sql.toString();
    }

    /**
     * Builds the SQL PRIMARY KEY clause from the provided column metadata for a table.
     *
     * @param columns   the list of column metadata to inspect for primary key columns
     * @param tableName the table name used in the error message if no primary key is found
     * @return          a SQL PRIMARY KEY clause listing the primary key column names, e.g. "PRIMARY KEY (id, col2)"
     * @throws RuntimeException if no columns are marked as primary key
     */
    private String generatePrimaryKey(List<ColumnMetadata> columns, String tableName) {
        List<String> primaryKeys = columns.stream()
                .filter(ColumnMetadata::isPrimaryKey)
                .map(ColumnMetadata::getName)
                .toList();
        if (primaryKeys.isEmpty()) {
            throw new RuntimeException("Table: " + tableName + " does not have a primary key");
        }
        return "PRIMARY KEY (" + String.join(", ", primaryKeys) + ")";
    }

    /**
     * Builds comma-prefixed UNIQUE constraint clauses for the given table suitable for inclusion
     * inside the body of a `CREATE TABLE` statement.
     *
     * @param tableMetadata metadata for the table whose unique key constraints will be generated;
     *                      expected to provide `getUniqueKeysConstraints()` and `getName()`
     * @return A string containing zero or more clauses of the form
     *         ", CONSTRAINT unique_<tableName>_<cols> UNIQUE (<col1>, <col2>, ...)".
     *         Returns an empty string if the table has no unique key constraints.
     */
    public String generateUniqueKeysConstraints(TableMetadata tableMetadata) {
        if (tableMetadata.getUniqueKeysConstraints() == null || tableMetadata.getUniqueKeysConstraints().isEmpty()) {
            return "";
        }
        StringBuilder addUniqueConstraintsStatements = new StringBuilder();
        for (Map.Entry<Integer, String[]> entry : tableMetadata.getUniqueKeysConstraints().entrySet()) {
            addUniqueConstraintsStatements.append(", CONSTRAINT unique_")
                    .append(tableMetadata.getName())
                    .append("_")
                    .append(String.join("_", entry.getValue()))
                    .append(" UNIQUE (")
                    .append(String.join(", ", entry.getValue()))
                    .append(")");
        }
        return addUniqueConstraintsStatements.toString();
    }

    /**
     * Generate ALTER TABLE statements that add foreign-key constraints for the given table.
     *
     * @param tableMetadata metadata of the table whose foreign-key columns will be converted into ALTER TABLE ... ADD CONSTRAINT ... FOREIGN KEY statements
     * @return a string containing the ALTER TABLE statement (possibly with multiple ADD CONSTRAINT clauses) that adds all foreign-key constraints for the table, or an empty string if the table has no foreign keys
     */
    public String generateForeignKeys(TableMetadata tableMetadata) {
        List<ColumnMetadata> foreignKeyColumns = tableMetadata.getColumnMetadata()
                .stream()
                .filter(c -> c.getForeignKeyTableReference() != null)
                .toList();
        if (foreignKeyColumns.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\nALTER TABLE ")
                .append(tableMetadata.getName())
                .append("\n");
        for (int i = 0; i < foreignKeyColumns.size(); i++) {
            ColumnMetadata foreignKey = foreignKeyColumns.get(i);
            sb.append("ADD CONSTRAINT fk_")
                    .append(tableMetadata.getName())
                    .append("_")
                    .append(foreignKey.getName())
                    .append(" FOREIGN KEY (")
                    .append(foreignKey.getName())
                    .append(") REFERENCES ")
                    .append(foreignKey.getForeignKeyTableReference())
                    .append("(id)");
            if (foreignKey.getForeignKeyOnDeleteAction() != null) {
                sb.append(" ON DELETE ")
                        .append(foreignKey.getForeignKeyOnDeleteAction());
            }
            if (i < foreignKeyColumns.size() - 1) {
                sb.append(",\n");
            } else {
                sb.append(";\n");
            }
        }
        return sb.toString();
    }

    /**
     * Generates CREATE INDEX statements for columns marked as indexes in the given table.
     *
     * @param tableMetadata metadata of the table whose columns will be inspected for index creation
     * @return a concatenated SQL string containing one CREATE INDEX statement per indexed column (each ending with ";\n"), or an empty string if the table has no indexed columns
     */
    public String generateIndexes(TableMetadata tableMetadata) {
        List<ColumnMetadata> indexColumns = tableMetadata.getColumnMetadata()
                .stream()
                .filter(ColumnMetadata::isIndex)
                .toList();
        if (indexColumns.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ColumnMetadata column : indexColumns) {
            sb.append("\nCREATE INDEX idx_")
                    .append(tableMetadata.getName())
                    .append("_")
                    .append(column.getName())
                    .append(" ON ")
                    .append(tableMetadata.getName())
                    .append("(")
                    .append(column.getName())
                    .append(");\n");
        }
        return sb.toString();
    }

    /**
     * Produces a SQL DEFAULT clause for the given column based on its configured default value.
     *
     * @param columnMetadata metadata describing the column and its possible default values
     * @return the DEFAULT clause (for example ` DEFAULT 'text'`, ` DEFAULT 0`, or ` DEFAULT TRUE`), or null if no default is configured
     */
    public String generateDefaultValue(ColumnMetadata columnMetadata) {
        StringBuilder defaultValue = new StringBuilder(" DEFAULT");
        if (columnMetadata.isDefaultFalse()) {
            defaultValue.append(" FALSE");
        } else if (columnMetadata.isDefaultTrue()) {
            defaultValue.append(" TRUE");
        } else if (columnMetadata.getDefaultDoubleValue() != null) {
            defaultValue.append(" ").append(columnMetadata.getDefaultDoubleValue());
        } else if (columnMetadata.getDefaultIntValue() != null) {
            defaultValue.append(" ").append(columnMetadata.getDefaultIntValue());
        } else if (columnMetadata.getDefaultFloatValue() != null) {
            defaultValue.append(" ").append(columnMetadata.getDefaultFloatValue());
        } else if (columnMetadata.getDefaultBigIntValue() != null) {
            defaultValue.append(" ").append(columnMetadata.getDefaultBigIntValue());
        } else if (columnMetadata.getDefaultStringValue() != null) {
            defaultValue.append(" '").append(columnMetadata.getDefaultStringValue()).append("'");
        } else {
            return null;
        }
        return defaultValue.toString();
    }
}
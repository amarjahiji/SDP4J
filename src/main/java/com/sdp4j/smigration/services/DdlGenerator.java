package com.sdp4j.smigration.services;


import com.sdp4j.smigration.metadata.ColumnMetadata;
import com.sdp4j.smigration.metadata.TableMetadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DdlGenerator {

    /**
     * Builds a CREATE TABLE IF NOT EXISTS statement from the provided table metadata.
     *
     * @param table metadata describing the table, its columns, keys, and constraints
     * @return the CREATE TABLE SQL statement including column definitions, the primary key, and any unique constraints
     */
    public String generateCreateTable(TableMetadata table) {
        String columnDdl = table.getColumnMetadata().stream()
                .map(this::generateColumn)
                .collect(Collectors.joining(",\n"));
        String primaryKeyDdl = generatePrimaryKey(table.getColumnMetadata(), table.getName());
        return "\nCREATE TABLE IF NOT EXISTS " + table.getName() + " (\n" +
                columnDdl +
                ",\n" + primaryKeyDdl +
                "\n" + generateUniqueKeysConstraints(table) +
                "\n);";
    }

    /**
     * Builds the SQL fragment that defines a single column for a CREATE TABLE statement.
     *
     * @param column metadata used to produce the column definition (name, type, nullability and default)
     * @return the SQL column definition fragment (for example: "col_name INT NOT NULL DEFAULT 0")
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
     * Builds the SQL PRIMARY KEY clause for the given table from its primary-key columns.
     *
     * @param columns   the list of column metadata to inspect for primary-key columns
     * @param tableName the table name (used in the error message if no primary key is found)
     * @return          the PRIMARY KEY clause, e.g. "PRIMARY KEY (id, other_id)"
     * @throws RuntimeException if the provided columns contain no primary-key column
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
     * Builds SQL UNIQUE constraint fragments for the table's unique key definitions.
     *
     * @param tableMetadata metadata for the table containing unique key definitions
     * @return a concatenated string of ", CONSTRAINT unique_<table>_<cols> UNIQUE (<cols>)" fragments for each unique key; returns an empty string if no unique keys are defined
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
     * Generates ALTER TABLE statements that add foreign key constraints for columns in the given table.
     *
     * @param tableMetadata metadata of the table whose columns will be inspected for foreign-key definitions
     * @return an SQL string beginning with "ALTER TABLE <tableName>" containing one or more
     *         "ADD CONSTRAINT ... FOREIGN KEY ..." clauses terminated by a semicolon, or an empty
     *         string if the table has no foreign-key columns
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
                    .append("(")
                    .append(foreignKey.getForeignKeyReferencedColumn())
                    .append(")");
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
     * Generates CREATE INDEX statements for columns marked as indexed in the provided table.
     *
     * @return a concatenated string with one index statement per indexed column formatted as
     *         "\nCREATE INDEX idx_<table>_<column> ON <table>(<column>);\n", or an empty string if no indexed columns exist.
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
     * Builds an SQL `DEFAULT` clause for the given column based on its configured default value.
     *
     * @param columnMetadata metadata that may contain one of the supported default indicators or values
     * @return the `DEFAULT ...` clause (starting with a leading space), or `null` if the column has no default
     */
    public String generateDefaultValue(ColumnMetadata columnMetadata) {
        StringBuilder defaultValue = new StringBuilder(" DEFAULT");
        if (columnMetadata.isDefaultFalse()) {
            defaultValue.append(" FALSE");
        } else if (columnMetadata.isDefaultTrue()) {
            defaultValue.append(" TRUE");
        } else if (columnMetadata.getDefaultDoublePrecisionValue() != null) {
            defaultValue.append(" ").append(columnMetadata.getDefaultDoublePrecisionValue());
        } else if (columnMetadata.getDefaultIntValue() != null) {
            defaultValue.append(" ").append(columnMetadata.getDefaultIntValue());
        } else if (columnMetadata.getDefaultRealValue() != null) {
            defaultValue.append(" ").append(columnMetadata.getDefaultRealValue());
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
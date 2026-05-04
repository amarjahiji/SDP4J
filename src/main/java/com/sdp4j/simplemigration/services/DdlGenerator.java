package com.sdp4j.simplemigration.services;


import com.sdp4j.simplemigration.metadata.ColumnMetadata;
import com.sdp4j.simplemigration.metadata.TableMetadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DdlGenerator {

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
            sb.append("\nCREATE INDEX IF NOT EXISTS idx_")
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
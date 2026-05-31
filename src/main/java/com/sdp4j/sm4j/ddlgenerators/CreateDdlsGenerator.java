package com.sdp4j.sm4j.ddlgenerators;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sm4j.metadata.ColumnMetadata;
import com.sdp4j.sm4j.metadata.TableMetadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CreateDdlsGenerator extends CommonDdlsGenerator {

    public String generateTableWithoutFkAndIdx(TableMetadata table) {
        String columnDdl = table.getColumnMetadata().stream()
                .map(this::generateColumn)
                .collect(Collectors.joining(",\n"));
        String primaryKeysDdl = generatePrimaryKeys(table.getColumnMetadata(), table.getName());
        return "\nCREATE TABLE IF NOT EXISTS " + table.getName() + " (\n" +
                columnDdl +
                ",\n" + primaryKeysDdl +
                "\n" + generateUniqueKeysConstraintsForNewTable(table) +
                "\n);";
    }

    private String generatePrimaryKeys(List<ColumnMetadata> columns, String tableName) {
        List<String> primaryKeys = columns.stream()
                .filter(ColumnMetadata::isPrimaryKey)
                .map(ColumnMetadata::getName)
                .toList();
        if (primaryKeys.isEmpty()) {
            throw new Sdp4jValidationException("Table: " + tableName + " does not have a primary key");
        }
        return "PRIMARY KEY (" + String.join(", ", primaryKeys) + ")";
    }

    private String generateUniqueKeysConstraintsForNewTable(TableMetadata tableMetadata) {
        if (tableMetadata.getUniqueKeysConstraints() == null || tableMetadata.getUniqueKeysConstraints().isEmpty()) {
            return "";
        }
        StringBuilder addUniqueConstraintsStatements = new StringBuilder();
        for (Map.Entry<Integer, String[]> entry : tableMetadata.getUniqueKeysConstraints().entrySet()) {
            String uniqueConstraintName = buildUniqueConstraintName(tableMetadata.getName(), entry.getValue());
            addUniqueConstraintsStatements.append(", CONSTRAINT ")
                    .append(uniqueConstraintName)
                    .append(" UNIQUE (")
                    .append(String.join(", ", entry.getValue()))
                    .append(")");
        }
        return addUniqueConstraintsStatements.toString();
    }
}

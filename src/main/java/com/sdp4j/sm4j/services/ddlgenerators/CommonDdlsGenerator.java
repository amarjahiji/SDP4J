package com.sdp4j.sm4j.services.ddlgenerators;


import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.sm4j.metadata.ColumnMetadata;

public class CommonDdlsGenerator {

    public String generateColumn(ColumnMetadata column) {
        StringBuilder ddl = new StringBuilder();
        ddl.append(column.getName())
                .append(" ")
                .append(column.getType());
        if (!column.isNullable()) {
            ddl.append(" NOT NULL");
        }
        String defaultValue = generateDefaultValue(column);
        if (defaultValue != null) {
            ddl.append(defaultValue);
        }
        return ddl.toString();
    }

    public void validateSqlIdentifiers(String operation, String... identifiers) {
        if (!CommonUtil.areValidSqlIdentifiers(identifiers)) {
            throw new Sdp4jValidationException("Invalid SQL identifier provided for " + operation);
        }
    }

    private String generateDefaultValue(ColumnMetadata columnMetadata) {
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

    public String buildForeignKeyName(String tableName, String... columnNames) {
        return "fk_" + tableName + "_" + String.join("_", columnNames);
    }

    public String buildIndexName(String tableName, String columnName) {
        return "idx_" + tableName + "_" + columnName;
    }

    public String buildUniqueConstraintName(String tableName, String... columnNames) {
        return "uq_" + tableName + "_" + String.join("_", columnNames);
    }
}

package com.sdp4j.sm4j.services.ddlgenerators;

import com.sdp4j.sm4j.metadata.ColumnMetadata;
import com.sdp4j.sm4j.metadata.TableMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddDdlsGenerator extends CommonDdlsGenerator {

    public String generateAddColumn(String tableName, ColumnMetadata column) {
        return "ALTER TABLE " + tableName + " ADD COLUMN " + generateColumn(column) + ";";
    }

    public List<String> generateAddForeignKeys(TableMetadata tableMetadata) {
        List<String> ddls = new ArrayList<>();
        for (ColumnMetadata column : tableMetadata.getColumnMetadata()) {
            generateAddForeignKey(tableMetadata.getName(), column).ifPresent(ddls::add);
        }
        return ddls;
    }

    public Optional<String> generateAddForeignKey(String tableName, ColumnMetadata column) {
        if (column.getForeignKeyTableReference() == null) {
            return Optional.empty();
        }
        StringBuilder ddl = new StringBuilder("ALTER TABLE ")
                .append(tableName)
                .append(" ADD CONSTRAINT fk_")
                .append(tableName)
                .append("_")
                .append(column.getName())
                .append(" FOREIGN KEY (")
                .append(column.getName())
                .append(") REFERENCES ")
                .append(column.getForeignKeyTableReference())
                .append("(")
                .append(column.getForeignKeyReferencedColumn())
                .append(")");
        if (column.getForeignKeyOnDeleteAction() != null) {
            ddl.append(" ON DELETE ").append(column.getForeignKeyOnDeleteAction());
        }
        ddl.append(";");
        return Optional.of(ddl.toString());
    }

    public List<String> generateIndicesDdls(TableMetadata tableMetadata) {
        List<String> ddls = new ArrayList<>();
        for (ColumnMetadata column : tableMetadata.getColumnMetadata()) {
            if (column.isIndex()) {
                ddls.add(generateAddIndex(tableMetadata.getName(), column.getName()));
            }
        }
        return ddls;
    }

    public String generateAddIndex(String tableName, String columnName) {
        String indexName = buildIndexName(tableName, columnName);
        return "CREATE INDEX IF NOT EXISTS " + indexName
                + " ON " + tableName + "(" + columnName + ");";
    }
}

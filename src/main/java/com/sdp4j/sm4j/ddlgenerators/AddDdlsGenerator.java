package com.sdp4j.sm4j.ddlgenerators;

import com.sdp4j.sm4j.metadata.ColumnMetadata;
import com.sdp4j.sm4j.metadata.TableMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AddDdlsGenerator extends CommonDdlsGenerator {

    public String generateAddColumn(String tableName, ColumnMetadata column) {
        return "ALTER TABLE " + tableName + " ADD COLUMN IF NOT EXISTS " + generateColumn(column) + ";";
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
        String constraintName = "fk_" + tableName + "_" + column.getName();
        StringBuilder alter = new StringBuilder("ALTER TABLE ")
                .append(tableName)
                .append(" ADD CONSTRAINT ")
                .append(constraintName)
                .append(" FOREIGN KEY (")
                .append(column.getName())
                .append(") REFERENCES ")
                .append(column.getForeignKeyTableReference())
                .append("(")
                .append(column.getForeignKeyReferencedColumn())
                .append(")");
        if (column.getForeignKeyOnDeleteAction() != null) {
            alter.append(" ON DELETE ").append(column.getForeignKeyOnDeleteAction());
        }
        String ddl = "DO $$ BEGIN"
                + " IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = '" + constraintName + "') THEN "
                + alter + ";"
                + " END IF;"
                + " END $$;";
        return Optional.of(ddl);
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
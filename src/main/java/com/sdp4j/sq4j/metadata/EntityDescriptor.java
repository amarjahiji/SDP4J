package com.sdp4j.sq4j.metadata;

import com.sdp4j.sm4j.metadata.ColumnMetadata;
import com.sdp4j.sm4j.metadata.TableMetadata;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public record EntityDescriptor(Class<?> javaClass, TableMetadata tableMetadata, Map<String, Field> fieldByColumn) {

    public String tableName() {
        return tableMetadata.getName();
    }

    public List<ColumnMetadata> columns() {
        return tableMetadata.getColumnMetadata();
    }

    public boolean hasColumn(String columnName) {
        return fieldByColumn.containsKey(columnName);
    }

    public Field fieldFor(String columnName) {
        return fieldByColumn.get(columnName);
    }

    public ColumnMetadata columnMetadata(String columnName) {
        for (ColumnMetadata column : tableMetadata.getColumnMetadata()) {
            if (column.getName().equals(columnName)) return column;
        }
        return null;
    }
}

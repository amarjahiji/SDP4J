package com.sdp4j.sm4j.metadata;

import java.util.List;
import java.util.Map;

public class TableMetadata {
    private String name;
    private List<ColumnMetadata> columnMetadata;
    private Map<Integer, String[]> uniqueKeysConstraints;
    private boolean snakeCase;

    public TableMetadata() {
    }

    public TableMetadata(String name, Map<Integer, String[]> uniqueKeysConstraints, boolean snakeCase) {
        this.name = name;
        this.uniqueKeysConstraints = uniqueKeysConstraints;
        this.snakeCase = snakeCase;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ColumnMetadata> getColumnMetadata() {
        return columnMetadata;
    }

    public void setColumnMetadata(List<ColumnMetadata> columnMetadata) {
        this.columnMetadata = columnMetadata;
    }

    public Map<Integer, String[]> getUniqueKeysConstraints() {
        return uniqueKeysConstraints;
    }

    public void setUniqueKeysConstraints(Map<Integer, String[]> uniqueKeysConstraints) {
        this.uniqueKeysConstraints = uniqueKeysConstraints;
    }

    public boolean isSnakeCase() {
        return snakeCase;
    }

    public void setSnakeCase(boolean snakeCase) {
        this.snakeCase = snakeCase;
    }
}
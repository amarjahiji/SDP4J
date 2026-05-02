package com.sdp4j.migration.metadata;

import java.util.List;
import java.util.Map;

public class TableMetadata {
    private String name;
    private List<ColumnMetadata> columnMetadata;
    private Map<Integer, String[]> uniqueKeysConstraints;
    private boolean snakeCase;

    /**
     * Creates an empty TableMetadata instance.
     *
     * All fields are left at their default values (object references are `null`, primitive `boolean` is `false`).
     */
    public TableMetadata() {
    }

    /**
     * Create a TableMetadata with the specified name, unique key constraints, and snake_case flag.
     *
     * @param name the table name
     * @param uniqueKeysConstraints a map of unique key constraint identifiers to arrays of column names
     * @param snakeCase true if column and constraint names should follow snake_case, false otherwise
     */
    public TableMetadata(String name, Map<Integer, String[]> uniqueKeysConstraints, boolean snakeCase) {
        this.name = name;
        this.uniqueKeysConstraints = uniqueKeysConstraints;
        this.snakeCase = snakeCase;
    }

    /**
     * Retrieve the table name.
     *
     * @return the table name, or {@code null} if it has not been set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the table's name.
     *
     * @param name the new table name, or {@code null} to unset it
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the list of column metadata associated with this table.
     *
     * @return the list of {@link ColumnMetadata} objects, or {@code null} if not set
     */
    public List<ColumnMetadata> getColumnMetadata() {
        return columnMetadata;
    }

    /**
     * Sets the list of column metadata associated with this table.
     *
     * @param columnMetadata the list of ColumnMetadata objects representing the table's columns
     */
    public void setColumnMetadata(List<ColumnMetadata> columnMetadata) {
        this.columnMetadata = columnMetadata;
    }

    /**
     * Retrieve the unique key constraints map keyed by index.
     *
     * @return the map where each key is an index and each value is an array of column names comprising that unique constraint; may be {@code null} if not set
     */
    public Map<Integer, String[]> getUniqueKeysConstraints() {
        return uniqueKeysConstraints;
    }

    /**
     * Sets the table's unique key constraints.
     *
     * @param uniqueKeysConstraints map where each key is a constraint identifier and the value is an array of column names that compose that unique constraint
     */
    public void setUniqueKeysConstraints(Map<Integer, String[]> uniqueKeysConstraints) {
        this.uniqueKeysConstraints = uniqueKeysConstraints;
    }

    /**
     * Indicates whether table identifiers use snake_case naming.
     *
     * @return `true` if identifiers use snake_case, `false` otherwise.
     */
    public boolean isSnakeCase() {
        return snakeCase;
    }

    /**
     * Sets whether table and column names should use snake_case naming.
     *
     * @param snakeCase true to enable snake_case naming, false to disable it
     */
    public void setSnakeCase(boolean snakeCase) {
        this.snakeCase = snakeCase;
    }
}
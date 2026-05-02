package com.sdp4j.smigration.metadata;

import java.util.List;
import java.util.Map;

public class TableMetadata {
    private String name;
    private List<ColumnMetadata> columnMetadata;
    private Map<Integer, String[]> uniqueKeysConstraints;
    private boolean snakeCase;

    /**
     * Constructs a new TableMetadata with no initial values.
     *
     * The instance's fields are left at their default values and should be set via the provided setters.
     */
    public TableMetadata() {
    }

    /**
     * Creates a TableMetadata with the specified table name, unique key constraints, and naming style flag.
     *
     * @param name the table name
     * @param uniqueKeysConstraints a map of unique key identifiers to arrays of column names that form each unique constraint
     * @param snakeCase true to indicate names should use snake_case, false to indicate otherwise
     */
    public TableMetadata(String name, Map<Integer, String[]> uniqueKeysConstraints, boolean snakeCase) {
        this.name = name;
        this.uniqueKeysConstraints = uniqueKeysConstraints;
        this.snakeCase = snakeCase;
    }

    /**
     * Gets the table name.
     *
     * @return the table name, or {@code null} if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Set the table's name.
     *
     * @param name the table name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Retrieve the list of column metadata associated with this table.
     *
     * @return the list of {@code ColumnMetadata} for this table, or {@code null} if not set
     */
    public List<ColumnMetadata> getColumnMetadata() {
        return columnMetadata;
    }

    /**
     * Sets the list of column metadata for this table.
     *
     * @param columnMetadata list of {@link ColumnMetadata} describing the table's columns, or `null` if not available
     */
    public void setColumnMetadata(List<ColumnMetadata> columnMetadata) {
        this.columnMetadata = columnMetadata;
    }

    /**
     * Access the map of unique key constraints defined for this table.
     *
     * @return the map that associates an integer identifier to an array of column names representing a unique constraint, or {@code null} if none are set
     */
    public Map<Integer, String[]> getUniqueKeysConstraints() {
        return uniqueKeysConstraints;
    }

    /**
     * Set the map of unique key constraints for the table.
     *
     * The map keys identify constraint groups and each map value is an array of column names that form
     * the unique constraint for that group.
     *
     * @param uniqueKeysConstraints map from group identifier to an array of column names representing a unique constraint
     */
    public void setUniqueKeysConstraints(Map<Integer, String[]> uniqueKeysConstraints) {
        this.uniqueKeysConstraints = uniqueKeysConstraints;
    }

    /**
     * Indicates whether names should be formatted in snake case.
     *
     * @return `true` if names should be formatted in snake case, `false` otherwise.
     */
    public boolean isSnakeCase() {
        return snakeCase;
    }

    /**
     * Configure whether table and column identifiers use snake_case naming.
     *
     * @param snakeCase true to enable snake_case naming for table and column identifiers, false to disable it
     */
    public void setSnakeCase(boolean snakeCase) {
        this.snakeCase = snakeCase;
    }
}
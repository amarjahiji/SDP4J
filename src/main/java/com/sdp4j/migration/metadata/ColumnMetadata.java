package com.sdp4j.migration.metadata;

public class ColumnMetadata {
    private String name;
    private String type;
    private boolean primaryKey;
    private boolean nullable = true;
    private Double defaultDoubleValue;
    private Integer defaultIntValue;
    private boolean defaultTrue;
    private boolean defaultFalse;
    private Float defaultFloatValue;
    private Long defaultBigIntValue;
    private String defaultStringValue;
    private String foreignKeyTableReference;
    private String foreignKeyOnDeleteAction;
    private boolean index;

    /**
     * Constructs a ColumnMetadata instance with default field values.
     *
     * By default `nullable` is true; boolean flags such as `primaryKey`, `defaultTrue`, `defaultFalse`,
     * and `index` are false. Typed default value fields (Double, Integer, Float, Long) and string
     * references (name, type, defaultStringValue, foreignKeyTableReference, foreignKeyOnDeleteAction)
     * are null.
     */
    public ColumnMetadata() {
    }

    /**
     * Create a ColumnMetadata initialized with the specified column name and data type.
     *
     * Other attributes (such as primaryKey, index, nullable, and typed default-value fields) remain at their default values.
     *
     * @param name the column name
     * @param type the column data type
     */
    public ColumnMetadata(String name, String type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Gets the column name.
     *
     * @return the column name, or {@code null} if it has not been set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the column name.
     *
     * @param name the column name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the column's data type.
     *
     * @return the column's data type string, or null if not set
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the column's data type.
     *
     * @param type the column data type (for example, "INTEGER" or "VARCHAR(255)")
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Indicates whether this column is a primary key.
     *
     * @return `true` if the column is a primary key, `false` otherwise.
     */
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    /**
     * Sets whether this column is a primary key.
     *
     * @param primaryKey true if the column is a primary key, false otherwise
     */
    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    /**
     * Indicates whether the column allows NULL values.
     *
     * @return `true` if the column allows NULL values, `false` otherwise.
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * Set whether the column allows NULL values.
     *
     * @param nullable true if the column should allow NULL values, false otherwise
     */
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    /**
     * Retrieves the column's default double value.
     *
     * @return the default Double for the column, or {@code null} if no default is set
     */
    public Double getDefaultDoubleValue() {
        return defaultDoubleValue;
    }

    /**
     * Sets the column's default value as a Double.
     *
     * @param defaultDoubleValue the default double value for the column, or {@code null} to clear it
     */
    public void setDefaultDoubleValue(Double defaultDoubleValue) {
        this.defaultDoubleValue = defaultDoubleValue;
    }

    /**
     * Retrieves the column's default integer value, if one is defined.
     *
     * @return Integer representing the column's default integer value, or `null` if none is set.
     */
    public Integer getDefaultIntValue() {
        return defaultIntValue;
    }

    /**
     * Set the default integer value for the column.
     *
     * @param defaultIntValue the default Integer to use for the column, or {@code null} if no default
     */
    public void setDefaultIntValue(Integer defaultIntValue) {
        this.defaultIntValue = defaultIntValue;
    }

    /**
     * Indicates whether the column's default value is true.
     *
     * @return true if the column's default is true, false otherwise.
     */
    public boolean isDefaultTrue() {
        return defaultTrue;
    }

    /**
     * Mark whether the column's default value is true.
     *
     * @param defaultTrue true if the column's default value should be true, false otherwise
     */
    public void setDefaultTrue(boolean defaultTrue) {
        this.defaultTrue = defaultTrue;
    }

    /**
     * Indicates whether the column's default value is explicitly false.
     *
     * @return `true` if the column's default value is false, `false` otherwise.
     */
    public boolean isDefaultFalse() {
        return defaultFalse;
    }

    /**
     * Sets whether the column's default value is false.
     *
     * @param defaultFalse `true` if the column's default value should be false, `false` otherwise
     */
    public void setDefaultFalse(boolean defaultFalse) {
        this.defaultFalse = defaultFalse;
    }

    /**
     * Gets the column's default float value.
     *
     * @return the default float value for the column, or {@code null} if no default is set
     */
    public Float getDefaultFloatValue() {
        return defaultFloatValue;
    }

    /**
     * Sets the column's default float value.
     *
     * @param defaultFloatValue the float value to use as the column's default, or {@code null} if the column has no default float
     */
    public void setDefaultFloatValue(Float defaultFloatValue) {
        this.defaultFloatValue = defaultFloatValue;
    }

    /**
     * Gets the default BigInt value for the column, if one is configured.
     *
     * @return the default BigInt value as a `Long`, or `null` if no default is set
     */
    public Long getDefaultBigIntValue() {
        return defaultBigIntValue;
    }

    /**
     * Sets the column's default 64-bit integer value.
     *
     * @param defaultBigIntValue the default value to use for this column; may be {@code null} to indicate no default
     */
    public void setDefaultBigIntValue(Long defaultBigIntValue) {
        this.defaultBigIntValue = defaultBigIntValue;
    }

    /**
     * Retrieve the column's default string value.
     *
     * @return the default string value for the column, or {@code null} if none is set
     */
    public String getDefaultStringValue() {
        return defaultStringValue;
    }

    /**
     * Sets the default string value for this column.
     *
     * @param defaultStringValue the string to use as the column's default value
     */
    public void setDefaultStringValue(String defaultStringValue) {
        this.defaultStringValue = defaultStringValue;
    }

    /**
     * Gets the referenced table name for this column's foreign key.
     *
     * @return the referenced table name, or {@code null} if no foreign key is defined
     */
    public String getForeignKeyTableReference() {
        return foreignKeyTableReference;
    }

    /**
     * Sets the referenced table for this column's foreign key.
     *
     * @param foreignKeyTableReference the table (or table reference) that this column references; may be null to clear the reference
     */
    public void setForeignKeyTableReference(String foreignKeyTableReference) {
        this.foreignKeyTableReference = foreignKeyTableReference;
    }

    /**
     * The ON DELETE action for the foreign key constraint associated with this column.
     *
     * @return the action (e.g., "CASCADE", "SET NULL", "RESTRICT") to apply when the referenced row is deleted, or `null` if none is specified
     */
    public String getForeignKeyOnDeleteAction() {
        return foreignKeyOnDeleteAction;
    }

    /**
     * Sets the referential action to apply when the referenced foreign key row is deleted.
     *
     * @param foreignKeyOnDeleteAction the database-level on-delete action as a string (for example "CASCADE", "SET NULL", "RESTRICT", or another database-specific value)
     */
    public void setForeignKeyOnDeleteAction(String foreignKeyOnDeleteAction) {
        this.foreignKeyOnDeleteAction = foreignKeyOnDeleteAction;
    }

    /**
     * Indicates whether the column is indexed.
     *
     * @return `true` if the column is indexed, `false` otherwise.
     */
    public boolean isIndex() {
        return index;
    }

    /**
     * Sets whether this column is indexed.
     *
     * @param index `true` if the column should be indexed, `false` otherwise
     */
    public void setIndex(boolean index) {
        this.index = index;
    }
}

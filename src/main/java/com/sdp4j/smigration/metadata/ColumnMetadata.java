package com.sdp4j.smigration.metadata;

public class ColumnMetadata {
    private String name;
    private String type;
    private boolean primaryKey;
    private boolean nullable = true;
    private Double defaultDoublePrecisionValue;
    private Integer defaultIntValue;
    private boolean defaultTrue;
    private boolean defaultFalse;
    private Float defaultRealValue;
    private Long defaultBigIntValue;
    private String defaultStringValue;
    private String foreignKeyTableReference;
    private String foreignKeyReferencedColumn;
    private String foreignKeyOnDeleteAction;
    private boolean index;

    /**
     * Constructs an empty ColumnMetadata instance with all fields left at their default values.
     *
     * The `nullable` flag is initialized to true; other fields remain at standard Java defaults (null for object types, false for booleans).
     */
    public ColumnMetadata() {
    }

    /**
     * Creates a ColumnMetadata initialized with the specified column name and data type.
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
     * @return the column name, or {@code null} if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the column.
     *
     * @param name the column name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the data type of the column.
     *
     * @return the column's data type as a String, or `null` if not set
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the column's data type.
     *
     * @param type the column data type (for example: "INTEGER", "VARCHAR(255)", "BOOLEAN")
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Indicates whether this column is a primary key.
     *
     * @return true if the column is a primary key, false otherwise.
     */
    public boolean isPrimaryKey() {
        return primaryKey;
    }

    /**
     * Sets whether this column is a primary key.
     *
     * @param primaryKey true if the column should be marked as a primary key, false otherwise
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
     * Sets whether the column allows NULL values.
     *
     * @param nullable `true` if the column allows NULL, `false` if the column must be NOT NULL
     */
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    /**
     * The column's default DOUBLE PRECISION value.
     *
     * @return the default DOUBLE PRECISION value for the column, or {@code null} if not set
     */
    public Double getDefaultDoublePrecisionValue() {
        return defaultDoublePrecisionValue;
    }

    /**
     * Sets the column's default double-precision numeric value.
     *
     * @param defaultDoublePrecisionValue the default value for this column's double-precision type, or null to unset it
     */
    public void setDefaultDoublePrecisionValue(Double defaultDoublePrecisionValue) {
        this.defaultDoublePrecisionValue = defaultDoublePrecisionValue;
    }

    /**
     * Gets the column's default integer value.
     *
     * @return the default Integer for the column, or {@code null} if no integer default is set
     */
    public Integer getDefaultIntValue() {
        return defaultIntValue;
    }

    /**
     * Sets the column's default integer value.
     *
     * @param defaultIntValue the default Integer value for the column, or null to clear it
     */
    public void setDefaultIntValue(Integer defaultIntValue) {
        this.defaultIntValue = defaultIntValue;
    }

    /**
     * Indicates whether the column's configured default value is `true`.
     *
     * @return `true` if the column's default value is true, `false` otherwise.
     */
    public boolean isDefaultTrue() {
        return defaultTrue;
    }

    /**
     * Marks or unmarks this column as having a default boolean value of true.
     *
     * @param defaultTrue `true` to indicate the column's default value is true, `false` to clear that flag
     */
    public void setDefaultTrue(boolean defaultTrue) {
        this.defaultTrue = defaultTrue;
    }

    /**
     * Reports whether the column's default value is false.
     *
     * @return `true` if the column's default is false, `false` otherwise.
     */
    public boolean isDefaultFalse() {
        return defaultFalse;
    }

    /**
     * Sets whether the column's default value is false.
     *
     * @param defaultFalse true if the column's default value is false, false otherwise
     */
    public void setDefaultFalse(boolean defaultFalse) {
        this.defaultFalse = defaultFalse;
    }

    /**
     * The configured default FLOAT/REAL value for the column, if specified.
     *
     * @return the column's default Float value, or {@code null} if no default is set
     */
    public Float getDefaultRealValue() {
        return defaultRealValue;
    }

    /**
     * Set the column's default REAL (floating-point) value.
     *
     * @param defaultRealValue the default FLOAT value to assign to the column, or {@code null} if none
     */
    public void setDefaultRealValue(Float defaultRealValue) {
        this.defaultRealValue = defaultRealValue;
    }

    /**
     * Default BigInt value for the column, or {@code null} if none is set.
     *
     * @return the default BigInt value, or {@code null} if not specified
     */
    public Long getDefaultBigIntValue() {
        return defaultBigIntValue;
    }

    /**
     * Sets the default 64-bit integer (BIGINT) value for this column.
     *
     * @param defaultBigIntValue the default `Long` value to assign as the column's BIGINT default, or `null` if no BIGINT default is set
     */
    public void setDefaultBigIntValue(Long defaultBigIntValue) {
        this.defaultBigIntValue = defaultBigIntValue;
    }

    /**
     * Gets the default string value for the column.
     *
     * @return the default string value, or {@code null} if none is set.
     */
    public String getDefaultStringValue() {
        return defaultStringValue;
    }

    /**
     * Sets the default string value for this column.
     *
     * @param defaultStringValue the default string value to assign to the column, or {@code null} if no default is set
     */
    public void setDefaultStringValue(String defaultStringValue) {
        this.defaultStringValue = defaultStringValue;
    }

    /**
     * Gets the referenced table name for this column's foreign key.
     *
     * @return the referenced table name, or {@code null} if the column has no foreign key reference
     */
    public String getForeignKeyTableReference() {
        return foreignKeyTableReference;
    }

    /**
     * Sets the name of the table referenced by this column's foreign key.
     *
     * @param foreignKeyTableReference the referenced table name, or {@code null} if there is no foreign key table
     */
    public void setForeignKeyTableReference(String foreignKeyTableReference) {
        this.foreignKeyTableReference = foreignKeyTableReference;
    }

    /**
     * Retrieves the name of the referenced column for this column's foreign key.
     *
     * @return the referenced column name, or {@code null} if no foreign key reference is set
     */
    public String getForeignKeyReferencedColumn() {
        return foreignKeyReferencedColumn;
    }

    /**
     * Sets the referenced column name for this column's foreign key relationship.
     *
     * @param foreignKeyReferencedColumn the name of the referenced column in the foreign table, or {@code null} to clear the reference
     */
    public void setForeignKeyReferencedColumn(String foreignKeyReferencedColumn) {
        this.foreignKeyReferencedColumn = foreignKeyReferencedColumn;
    }

    /**
     * Foreign key ON DELETE action for this column.
     *
     * @return the foreign key ON DELETE action, or {@code null} if not set
     */
    public String getForeignKeyOnDeleteAction() {
        return foreignKeyOnDeleteAction;
    }

    /**
     * Sets the referential action to apply when the referenced row is deleted.
     *
     * @param foreignKeyOnDeleteAction the action to perform on delete (for example "CASCADE", "SET NULL", "RESTRICT"), or null to clear the setting
     */
    public void setForeignKeyOnDeleteAction(String foreignKeyOnDeleteAction) {
        this.foreignKeyOnDeleteAction = foreignKeyOnDeleteAction;
    }

    /**
     * Indicates whether the column has an index.
     *
     * @return `true` if the column is indexed, `false` otherwise.
     */
    public boolean isIndex() {
        return index;
    }

    /**
     * Sets whether the column should be indexed.
     *
     * @param index true to mark the column as indexed, false to mark it as not indexed
     */
    public void setIndex(boolean index) {
        this.index = index;
    }
}

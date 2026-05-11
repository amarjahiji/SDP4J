package com.sdp4j.sm4j.metadata;

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

    public ColumnMetadata() {
    }

    public ColumnMetadata(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public Double getDefaultDoublePrecisionValue() {
        return defaultDoublePrecisionValue;
    }

    public void setDefaultDoublePrecisionValue(Double defaultDoublePrecisionValue) {
        this.defaultDoublePrecisionValue = defaultDoublePrecisionValue;
    }

    public Integer getDefaultIntValue() {
        return defaultIntValue;
    }

    public void setDefaultIntValue(Integer defaultIntValue) {
        this.defaultIntValue = defaultIntValue;
    }

    public boolean isDefaultTrue() {
        return defaultTrue;
    }

    public void setDefaultTrue(boolean defaultTrue) {
        this.defaultTrue = defaultTrue;
    }

    public boolean isDefaultFalse() {
        return defaultFalse;
    }

    public void setDefaultFalse(boolean defaultFalse) {
        this.defaultFalse = defaultFalse;
    }

    public Float getDefaultRealValue() {
        return defaultRealValue;
    }

    public void setDefaultRealValue(Float defaultRealValue) {
        this.defaultRealValue = defaultRealValue;
    }

    public Long getDefaultBigIntValue() {
        return defaultBigIntValue;
    }

    public void setDefaultBigIntValue(Long defaultBigIntValue) {
        this.defaultBigIntValue = defaultBigIntValue;
    }

    public String getDefaultStringValue() {
        return defaultStringValue;
    }

    public void setDefaultStringValue(String defaultStringValue) {
        this.defaultStringValue = defaultStringValue;
    }

    public String getForeignKeyTableReference() {
        return foreignKeyTableReference;
    }

    public void setForeignKeyTableReference(String foreignKeyTableReference) {
        this.foreignKeyTableReference = foreignKeyTableReference;
    }

    public String getForeignKeyReferencedColumn() {
        return foreignKeyReferencedColumn;
    }

    public void setForeignKeyReferencedColumn(String foreignKeyReferencedColumn) {
        this.foreignKeyReferencedColumn = foreignKeyReferencedColumn;
    }

    public String getForeignKeyOnDeleteAction() {
        return foreignKeyOnDeleteAction;
    }

    public void setForeignKeyOnDeleteAction(String foreignKeyOnDeleteAction) {
        this.foreignKeyOnDeleteAction = foreignKeyOnDeleteAction;
    }

    public boolean isIndex() {
        return index;
    }

    public void setIndex(boolean index) {
        this.index = index;
    }
}

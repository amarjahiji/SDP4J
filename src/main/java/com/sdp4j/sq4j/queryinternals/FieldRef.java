package com.sdp4j.sq4j.queryinternals;

public class FieldRef {

    public static final FieldRef STAR = new FieldRef(null, "*", null, true);

    private final String tableQualifier;
    private final String column;
    private final String alias;
    private final boolean star;

    public FieldRef(String column) {
        this(null, column, null, false);
    }

    public FieldRef(String tableQualifier, String column) {
        this(tableQualifier, column, null, false);
    }

    public FieldRef(String tableQualifier, String column, String alias) {
        this(tableQualifier, column, alias, false);
    }

    private FieldRef(String tableQualifier, String column, String alias, boolean star) {
        this.tableQualifier = tableQualifier;
        this.column = column;
        this.alias = alias;
        this.star = star;
    }

    public String tableQualifier() {
        return tableQualifier;
    }

    public String column() {
        return column;
    }

    public String alias() {
        return alias;
    }

    public boolean isQualified() {
        return tableQualifier != null;
    }

    public boolean hasAlias() {
        return alias != null;
    }

    public boolean isStar() {
        return star;
    }
}

package com.sdp4j.sq4j.queryinternals;

public class FieldRef {

    public static final FieldRef STAR = new FieldRef(null, "*", true);

    private final String tableQualifier;
    private final String column;
    private final boolean star;

    public FieldRef(String column) {
        this(null, column, false);
    }

    public FieldRef(String tableQualifier, String column) {
        this(tableQualifier, column, false);
    }

    private FieldRef(String tableQualifier, String column, boolean star) {
        this.tableQualifier = tableQualifier;
        this.column = column;
        this.star = star;
    }

    public String tableQualifier() {
        return tableQualifier;
    }

    public String column() {
        return column;
    }

    public boolean isQualified() {
        return tableQualifier != null;
    }

    public boolean isStar() {
        return star;
    }
}

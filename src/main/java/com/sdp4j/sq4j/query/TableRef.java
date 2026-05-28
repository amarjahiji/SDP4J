package com.sdp4j.sq4j.query;

public class TableRef {

    private final String name;
    private final String alias;

    public TableRef(String name) {
        this(name, null);
    }

    public TableRef(String name, String alias) {
        this.name = name;
        this.alias = alias;
    }

    public String name() { return name; }

    public String alias() { return alias; }

    public String effectiveName() { return alias != null ? alias : name; }
}

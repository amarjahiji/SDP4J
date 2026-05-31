package com.sdp4j.sq4j.queryinternals;

public record TableRef(String name, String alias) {

    public TableRef(String name) {
        this(name, null);
    }

    public String effectiveName() {
        return alias != null ? alias : name;
    }
}

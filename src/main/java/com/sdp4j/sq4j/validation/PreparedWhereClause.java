package com.sdp4j.sq4j.validation;

import java.util.List;

public class PreparedWhereClause {

    private final String renderedSql;
    private final List<Object> orderedBindings;

    public PreparedWhereClause(String renderedSql, List<Object> orderedBindings) {
        this.renderedSql = renderedSql;
        this.orderedBindings = orderedBindings;
    }

    public String renderedSql() {
        return renderedSql;
    }

    public List<Object> orderedBindings() {
        return orderedBindings;
    }
}

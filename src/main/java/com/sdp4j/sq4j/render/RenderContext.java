package com.sdp4j.sq4j.render;

import java.util.ArrayList;
import java.util.List;

public class RenderContext {

    private final SqlDialect dialect;
    private final StringBuilder sql = new StringBuilder();
    private final List<Object> bindings = new ArrayList<>();

    public RenderContext(SqlDialect dialect) {
        this.dialect = dialect;
    }

    public SqlDialect dialect() {
        return dialect;
    }

    public RenderContext append(String s) {
        sql.append(s);
        return this;
    }

    public RenderContext bind(Object value) {
        bindings.add(value);
        sql.append(dialect.placeholder(bindings.size()));
        return this;
    }

    public RenderContext addBinding(Object value) {
        bindings.add(value);
        return this;
    }

    public String sql() {
        return sql.toString();
    }

    public List<Object> bindings() {
        return bindings;
    }
}
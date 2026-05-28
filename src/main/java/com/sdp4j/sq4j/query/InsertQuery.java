package com.sdp4j.sq4j.query;

import com.sdp4j.sq4j.render.InsertRenderer;
import com.sdp4j.sq4j.render.RenderContext;

import java.util.Collections;
import java.util.List;

public class InsertQuery implements Query {

    private final TableRef target;
    private final List<String> columns;
    private final List<Object> values;

    public InsertQuery(TableRef target, List<String> columns, List<Object> values) {
        this.target = target;
        this.columns = Collections.unmodifiableList(columns);
        this.values = Collections.unmodifiableList(values);
    }

    public TableRef target() { return target; }
    public List<String> columns() { return columns; }
    public List<Object> values() { return values; }

    @Override
    public void render(RenderContext ctx) {
        new InsertRenderer().render(this, ctx);
    }
}

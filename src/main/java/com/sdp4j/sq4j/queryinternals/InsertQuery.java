package com.sdp4j.sq4j.queryinternals;

import com.sdp4j.sq4j.renderers.InsertRenderer;
import com.sdp4j.sq4j.renderers.RenderContext;

import java.util.Collections;
import java.util.List;

public record InsertQuery(TableRef target, List<String> columns, List<Object> values) implements Query {

    public InsertQuery(TableRef target, List<String> columns, List<Object> values) {
        this.target = target;
        this.columns = Collections.unmodifiableList(columns);
        this.values = Collections.unmodifiableList(values);
    }

    @Override
    public void render(RenderContext ctx) {
        new InsertRenderer().render(this, ctx);
    }
}

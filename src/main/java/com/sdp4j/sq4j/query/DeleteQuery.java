package com.sdp4j.sq4j.query;

import com.sdp4j.sq4j.render.DeleteRenderer;
import com.sdp4j.sq4j.render.RenderContext;

import java.util.Collections;
import java.util.List;

public class DeleteQuery implements Query {

    private final TableRef from;
    private final String whereSql;
    private final List<Object> whereBindings;

    public DeleteQuery(TableRef from, String whereSql, List<Object> whereBindings) {
        this.from = from;
        this.whereSql = whereSql;
        this.whereBindings = whereBindings == null ? List.of() : Collections.unmodifiableList(whereBindings);
    }

    public TableRef from() { return from; }
    public String whereSql() { return whereSql; }
    public List<Object> whereBindings() { return whereBindings; }

    @Override
    public void render(RenderContext ctx) {
        new DeleteRenderer().render(this, ctx);
    }
}

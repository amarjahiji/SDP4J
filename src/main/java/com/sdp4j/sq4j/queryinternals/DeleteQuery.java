package com.sdp4j.sq4j.queryinternals;

import com.sdp4j.sq4j.renderers.DeleteRenderer;
import com.sdp4j.sq4j.renderers.RenderContext;

import java.util.Collections;
import java.util.List;

public record DeleteQuery(TableRef from, String whereSql, List<Object> whereBindings) implements Query {

    public DeleteQuery(TableRef from, String whereSql, List<Object> whereBindings) {
        this.from = from;
        this.whereSql = whereSql;
        this.whereBindings = whereBindings == null ? List.of() : Collections.unmodifiableList(whereBindings);
    }

    @Override
    public void render(RenderContext ctx) {
        new DeleteRenderer().render(this, ctx);
    }
}

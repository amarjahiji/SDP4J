package com.sdp4j.sq4j.query;

import com.sdp4j.sq4j.render.DeleteRenderer;
import com.sdp4j.sq4j.render.RenderContext;

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

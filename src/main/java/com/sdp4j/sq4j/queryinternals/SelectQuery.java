package com.sdp4j.sq4j.queryinternals;

import com.sdp4j.sq4j.renderers.RenderContext;
import com.sdp4j.sq4j.renderers.SelectRenderer;

import java.util.Collections;
import java.util.List;

public record SelectQuery(boolean distinct, List<FieldRef> fields, TableRef from, List<JoinSpec> joins, String whereSql,
                          List<Object> whereBindings, String orderBySql, Integer limit,
                          Integer offset) implements Query {

    public SelectQuery(boolean distinct,
                       List<FieldRef> fields,
                       TableRef from,
                       List<JoinSpec> joins,
                       String whereSql,
                       List<Object> whereBindings,
                       String orderBySql,
                       Integer limit,
                       Integer offset) {
        this.distinct = distinct;
        this.fields = Collections.unmodifiableList(fields);
        this.from = from;
        this.joins = joins == null ? List.of() : Collections.unmodifiableList(joins);
        this.whereSql = whereSql;
        this.whereBindings = whereBindings == null ? List.of() : Collections.unmodifiableList(whereBindings);
        this.orderBySql = orderBySql;
        this.limit = limit;
        this.offset = offset;
    }

    @Override
    public void render(RenderContext ctx) {
        new SelectRenderer().render(this, ctx);
    }
}

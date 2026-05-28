package com.sdp4j.sq4j.query;

import com.sdp4j.sq4j.render.RenderContext;
import com.sdp4j.sq4j.render.UpdateRenderer;

import java.util.Collections;
import java.util.List;

public record UpdateQuery(TableRef target, List<String> columnsToSet, List<Object> setValues, String whereSql,
                          List<Object> whereBindings) implements Query {

    public UpdateQuery(TableRef target,
                       List<String> columnsToSet,
                       List<Object> setValues,
                       String whereSql,
                       List<Object> whereBindings) {
        this.target = target;
        this.columnsToSet = Collections.unmodifiableList(columnsToSet);
        this.setValues = Collections.unmodifiableList(setValues);
        this.whereSql = whereSql;
        this.whereBindings = whereBindings == null ? List.of() : Collections.unmodifiableList(whereBindings);
    }

    @Override
    public void render(RenderContext ctx) {
        new UpdateRenderer().render(this, ctx);
    }
}

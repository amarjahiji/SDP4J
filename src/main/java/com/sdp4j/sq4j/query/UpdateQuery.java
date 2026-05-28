package com.sdp4j.sq4j.query;

import com.sdp4j.sq4j.render.RenderContext;
import com.sdp4j.sq4j.render.UpdateRenderer;

import java.util.Collections;
import java.util.List;

public class UpdateQuery implements Query {

    private final TableRef target;
    private final List<String> columnsToSet;
    private final List<Object> setValues;
    private final String whereSql;
    private final List<Object> whereBindings;

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

    public TableRef target() { return target; }
    public List<String> columnsToSet() { return columnsToSet; }
    public List<Object> setValues() { return setValues; }
    public String whereSql() { return whereSql; }
    public List<Object> whereBindings() { return whereBindings; }

    @Override
    public void render(RenderContext ctx) {
        new UpdateRenderer().render(this, ctx);
    }
}

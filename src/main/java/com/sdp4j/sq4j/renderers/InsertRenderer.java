package com.sdp4j.sq4j.renderers;

import com.sdp4j.sq4j.queryinternals.InsertQuery;

import java.util.List;

public class InsertRenderer {

    public void render(InsertQuery query, RenderContext ctx) {
        ctx.append("INSERT INTO ").append(query.target().name());
        ctx.append(" (");
        renderColumnList(query.columns(), ctx);
        ctx.append(") VALUES (");
        renderPlaceholders(query.values(), ctx);
        ctx.append(")");
    }

    private void renderColumnList(List<String> columns, RenderContext ctx) {
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) ctx.append(", ");
            ctx.append(columns.get(i));
        }
    }

    private void renderPlaceholders(List<Object> values, RenderContext ctx) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) ctx.append(", ");
            ctx.bind(values.get(i));
        }
    }
}

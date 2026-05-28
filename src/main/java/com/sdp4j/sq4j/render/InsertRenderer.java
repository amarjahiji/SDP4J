package com.sdp4j.sq4j.render;

import com.sdp4j.sq4j.query.InsertQuery;

import java.util.List;

public class InsertRenderer {

    public void render(InsertQuery query, RenderContext ctx) {
        SqlDialect dialect = ctx.dialect();
        ctx.append("INSERT INTO ").append(dialect.quoteIdentifier(query.target().name()));
        ctx.append(" (");
        renderColumnList(query.columns(), ctx);
        ctx.append(") VALUES (");
        renderPlaceholders(query.values(), ctx);
        ctx.append(")");
    }

    private void renderColumnList(List<String> columns, RenderContext ctx) {
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) ctx.append(", ");
            ctx.append(ctx.dialect().quoteIdentifier(columns.get(i)));
        }
    }

    private void renderPlaceholders(List<Object> values, RenderContext ctx) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) ctx.append(", ");
            ctx.bind(values.get(i));
        }
    }
}

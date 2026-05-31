package com.sdp4j.sq4j.renderers;

import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.sq4j.queryinternals.UpdateQuery;

import java.util.List;

public class UpdateRenderer {

    public void render(UpdateQuery query, RenderContext ctx) {
        ctx.append("UPDATE ").append(query.target().name());
        if (query.target().alias() != null) {
            ctx.append(" ").append(query.target().alias());
        }
        ctx.append(" SET ");
        renderSetClause(query.columnsToSet(), query.setValues(), ctx);
        if (CommonUtil.isValidString(query.whereSql())) {
            ctx.append(" WHERE ").append(query.whereSql());
            for (Object binding : query.whereBindings()) {
                ctx.addBinding(binding);
            }
        }
    }

    private void renderSetClause(List<String> columns, List<Object> values, RenderContext ctx) {
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) ctx.append(", ");
            ctx.append(columns.get(i)).append(" = ");
            ctx.bind(values.get(i));
        }
    }
}

package com.sdp4j.sq4j.renderers;

import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.sq4j.queryinternals.DeleteQuery;
import com.sdp4j.sq4j.queryinternals.TableRef;

public class DeleteRenderer {

    public void render(DeleteQuery query, RenderContext ctx) {
        ctx.append("DELETE FROM ");
        renderTable(query.from(), ctx);
        if (CommonUtil.isValidString(query.whereSql())) {
            ctx.append(" WHERE ").append(query.whereSql());
            for (Object binding : query.whereBindings()) {
                ctx.addBinding(binding);
            }
        }
    }

    private void renderTable(TableRef table, RenderContext ctx) {
        ctx.append(table.name());
        if (table.alias() != null) {
            ctx.append(" ").append(table.alias());
        }
    }
}

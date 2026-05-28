package com.sdp4j.sq4j.render;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.sq4j.query.FieldRef;
import com.sdp4j.sq4j.query.JoinSpec;
import com.sdp4j.sq4j.query.SelectQuery;
import com.sdp4j.sq4j.query.TableRef;

import java.util.List;

public class SelectRenderer {

    public void render(SelectQuery query, RenderContext ctx) {
        SqlDialect dialect = ctx.dialect();
        ctx.append("SELECT ");
        if (query.distinct()) {
            ctx.append("DISTINCT ");
        }
        renderProjectedFields(query.fields(), ctx);
        ctx.append(" FROM ");
        renderTable(query.from(), ctx);
        renderJoins(query.joins(), ctx);
        if (CommonUtil.isValidString(query.whereSql())) {
            ctx.append(" WHERE ").append(query.whereSql());
            for (Object binding : query.whereBindings()) {
                ctx.addBinding(binding);
            }
        }
        if (CommonUtil.isValidString(query.orderBySql())) {
            ctx.append(" ORDER BY ").append(query.orderBySql());
        }
        ctx.append(dialect.renderLimit(query.limit(), query.offset()));
    }

    private void renderProjectedFields(List<FieldRef> fields, RenderContext ctx) {
        if (fields.isEmpty()) {
            throw new Sdp4jValidationException(
                    "SELECT must project at least one column. Use select(...) with column names or \"*\"");
        }
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) ctx.append(", ");
            FieldRef field = fields.get(i);
            if (field.isStar()) {
                ctx.append("*");
                continue;
            }
            if (field.isQualified()) {
                ctx.append(ctx.dialect().quoteIdentifier(field.tableQualifier())).append(".");
            }
            ctx.append(ctx.dialect().quoteIdentifier(field.column()));
        }
    }

    private void renderTable(TableRef table, RenderContext ctx) {
        ctx.append(ctx.dialect().quoteIdentifier(table.name()));
        if (table.alias() != null) {
            ctx.append(" ").append(ctx.dialect().quoteIdentifier(table.alias()));
        }
    }

    private void renderJoins(List<JoinSpec> joins, RenderContext ctx) {
        for (JoinSpec join : joins) {
            ctx.append(" ").append(join.type().sql()).append(" ");
            renderTable(join.joinedTable(), ctx);
            ctx.append(" ON ").append(join.onSql());
            for (Object binding : join.onBindings()) {
                ctx.addBinding(binding);
            }
        }
    }
}
package com.sdp4j.sq4j.renderers;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.sq4j.queryinternals.FieldRef;
import com.sdp4j.sq4j.queryinternals.JoinSpec;
import com.sdp4j.sq4j.queryinternals.SelectQuery;
import com.sdp4j.sq4j.queryinternals.TableRef;

import java.util.List;

public class SelectRenderer {

    public void render(SelectQuery query, RenderContext ctx) {
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
        if (query.limit() != null) {
            ctx.append(" LIMIT ").append(String.valueOf(query.limit()));
        }
        if (query.offset() != null) {
            ctx.append(" OFFSET ").append(String.valueOf(query.offset()));
        }
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
                ctx.append(field.tableQualifier()).append(".");
            }
            ctx.append(field.column());
            if (field.hasAlias()) {
                ctx.append(" AS ").append(field.alias());
            }
        }
    }

    private void renderTable(TableRef table, RenderContext ctx) {
        ctx.append(table.name());
        if (table.alias() != null) {
            ctx.append(" ").append(table.alias());
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
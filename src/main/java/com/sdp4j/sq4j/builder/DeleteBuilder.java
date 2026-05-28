package com.sdp4j.sq4j.builder;

import com.sdp4j.sq4j.dsl.DeleteExecuteStep;
import com.sdp4j.sq4j.dsl.DeleteWhereStep;
import com.sdp4j.sq4j.execute.QueryExecutor;
import com.sdp4j.sq4j.metadata.EntityDescriptor;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;
import com.sdp4j.sq4j.query.DeleteQuery;
import com.sdp4j.sq4j.query.TableRef;
import com.sdp4j.sq4j.render.RenderContext;
import com.sdp4j.sq4j.render.SqlDialect;
import com.sdp4j.sq4j.validation.FromScope;
import com.sdp4j.sq4j.validation.PreparedWhereClause;
import com.sdp4j.sq4j.validation.WhereClauseValidator;

import java.util.List;

public class DeleteBuilder implements DeleteWhereStep, DeleteExecuteStep {

    private final QueryExecutor queryExecutor;
    private final SqlDialect dialect;
    private final WhereClauseValidator sqlFragmentValidator = new WhereClauseValidator();

    private final TableRef targetTable;
    private final FromScope fromScope = new FromScope();

    private String renderedWhereSql;
    private List<Object> whereBindings = List.of();

    public DeleteBuilder(EntityMetadataCache entityMetadataCache, QueryExecutor queryExecutor, SqlDialect dialect,
                         Class<?> entityClass, String alias) {
        this.queryExecutor = queryExecutor;
        this.dialect = dialect;
        EntityDescriptor descriptor = entityMetadataCache.metadataFor(entityClass);
        this.targetTable = new TableRef(descriptor.tableName(), alias);
        String qualifier = alias != null ? alias : descriptor.tableName();
        fromScope.register(qualifier, descriptor);
    }

    @Override
    public DeleteExecuteStep where(String sql, Object... values) {
        PreparedWhereClause prepared = sqlFragmentValidator.prepare(sql, values, fromScope);
        this.renderedWhereSql = prepared.renderedSql();
        this.whereBindings = prepared.orderedBindings();
        return this;
    }

    @Override
    public DeleteQuery toQuery() {
        return new DeleteQuery(targetTable, renderedWhereSql, whereBindings);
    }

    @Override
    public int execute() {
        RenderContext ctx = new RenderContext(dialect);
        toQuery().render(ctx);
        return queryExecutor.executeUpdate(ctx.sql(), ctx.bindings());
    }
}

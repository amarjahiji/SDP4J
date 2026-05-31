package com.sdp4j.sq4j.builders;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sq4j.steps.DeleteBindStep;
import com.sdp4j.sq4j.steps.DeleteExecuteStep;
import com.sdp4j.sq4j.steps.DeleteWhereStep;
import com.sdp4j.sq4j.executors.QueryExecutor;
import com.sdp4j.sq4j.metadata.EntityDescriptor;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;
import com.sdp4j.sq4j.queryinternals.DeleteQuery;
import com.sdp4j.sq4j.queryinternals.TableRef;
import com.sdp4j.sq4j.renderers.RenderContext;
import com.sdp4j.sq4j.validations.FromScope;
import com.sdp4j.sq4j.validations.PreparedWhereClause;
import com.sdp4j.sq4j.validations.WhereClauseValidator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeleteBuilder implements DeleteWhereStep, DeleteBindStep, DeleteExecuteStep {

    private final QueryExecutor queryExecutor;
    private final WhereClauseValidator sqlFragmentValidator = new WhereClauseValidator();

    private final TableRef targetTable;
    private final FromScope fromScope = new FromScope();

    private String namedWhereSql;
    private final Map<String, Object> whereNamedValues = new LinkedHashMap<>();
    private String renderedWhereSql;
    private List<Object> whereBindings = List.of();

    public DeleteBuilder(EntityMetadataCache entityMetadataCache, QueryExecutor queryExecutor,
                         Class<?> entityClass, String alias) {
        this.queryExecutor = queryExecutor;
        EntityDescriptor descriptor = entityMetadataCache.metadataFor(entityClass);
        this.targetTable = new TableRef(descriptor.tableName(), alias);
        String qualifier = alias != null ? alias : descriptor.tableName();
        fromScope.register(qualifier, descriptor);
    }

    @Override
    public DeleteBindStep where(String sql) {
        this.namedWhereSql = sql;
        return this;
    }

    @Override
    public DeleteBindStep set(String name, Object value) {
        if (name == null || !name.startsWith(":")) {
            throw new Sdp4jValidationException(
                    "Parameter name must start with ':' (e.g. \":id\"), got: " + name);
        }
        whereNamedValues.put(name, value);
        return this;
    }

    @Override
    public DeleteQuery toQuery() {
        resolveWhereClause();
        return new DeleteQuery(targetTable, renderedWhereSql, whereBindings);
    }

    @Override
    public int execute() {
        RenderContext ctx = new RenderContext();
        toQuery().render(ctx);
        return queryExecutor.executeUpdate(ctx.sql(), ctx.bindings());
    }

    private void resolveWhereClause() {
        if (namedWhereSql == null) {
            verifyAllBindingsUsed(Set.of());
            return;
        }
        PreparedWhereClause prepared = sqlFragmentValidator.prepareNamed(namedWhereSql, whereNamedValues, fromScope);
        this.renderedWhereSql = prepared.renderedSql();
        this.whereBindings = prepared.orderedBindings();
        verifyAllBindingsUsed(prepared.usedNames());
    }

    private void verifyAllBindingsUsed(Set<String> usedNames) {
        for (String key : whereNamedValues.keySet()) {
            if (!usedNames.contains(key)) {
                throw new Sdp4jValidationException(
                        "Bound parameter '" + key + "' does not appear in the WHERE clause");
            }
        }
    }
}

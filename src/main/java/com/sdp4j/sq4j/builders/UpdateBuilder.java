package com.sdp4j.sq4j.builders;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.core.exception.Sq4jException;
import com.sdp4j.sm4j.metadata.ColumnMetadata;
import com.sdp4j.sq4j.steps.UpdateBindStep;
import com.sdp4j.sq4j.steps.UpdateExecuteStep;
import com.sdp4j.sq4j.steps.UpdateSetStep;
import com.sdp4j.sq4j.steps.UpdateWhereStep;
import com.sdp4j.sq4j.executors.QueryExecutor;
import com.sdp4j.sq4j.metadata.EntityDescriptor;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;
import com.sdp4j.sq4j.queryinternals.TableRef;
import com.sdp4j.sq4j.queryinternals.UpdateQuery;
import com.sdp4j.sq4j.renderers.RenderContext;
import com.sdp4j.sq4j.validations.FromScope;
import com.sdp4j.sq4j.validations.PreparedWhereClause;
import com.sdp4j.sq4j.validations.WhereClauseValidator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UpdateBuilder<T> implements UpdateSetStep<T>, UpdateWhereStep, UpdateBindStep, UpdateExecuteStep {

    private final QueryExecutor queryExecutor;
    private final WhereClauseValidator sqlFragmentValidator = new WhereClauseValidator();

    private final Class<T> entityClass;
    private final EntityDescriptor entityDescriptor;
    private final TableRef targetTable;
    private final FromScope fromScope = new FromScope();

    private List<String> columnsToSet;
    private List<Object> setValues;
    private String namedWhereSql;
    private final Map<String, Object> whereNamedValues = new LinkedHashMap<>();
    private String renderedWhereSql;
    private List<Object> whereBindings = List.of();

    public UpdateBuilder(EntityMetadataCache entityMetadataCache, QueryExecutor queryExecutor,
                         Class<T> entityClass, String alias) {
        this.queryExecutor = queryExecutor;
        this.entityClass = entityClass;
        this.entityDescriptor = entityMetadataCache.metadataFor(entityClass);
        this.targetTable = new TableRef(entityDescriptor.tableName(), alias);
        String qualifier = alias != null ? alias : entityDescriptor.tableName();
        fromScope.register(qualifier, entityDescriptor);
    }

    @Override
    public UpdateWhereStep set(T entity) {
        if (entity == null) {
            throw new Sdp4jValidationException("Cannot update from a null entity");
        }
        extractNonNullNonPrimaryKeyColumns(entity);
        if (columnsToSet.isEmpty()) {
            throw new Sdp4jValidationException(
                    "Cannot update '" + entityDescriptor.tableName()
                            + "': no non-primary-key field on the supplied "
                            + entityClass.getSimpleName() + " instance is set");
        }
        return this;
    }

    @Override
    public UpdateBindStep where(String sql) {
        this.namedWhereSql = sql;
        return this;
    }

    @Override
    public UpdateBindStep set(String name, Object value) {
        if (name == null || !name.startsWith(":")) {
            throw new Sdp4jValidationException(
                    "Parameter name must start with ':' (e.g. \":id\"), got: " + name);
        }
        whereNamedValues.put(name, value);
        return this;
    }

    @Override
    public UpdateQuery toQuery() {
        if (columnsToSet == null) {
            throw new IllegalStateException("set(entity) must be called before toQuery()");
        }
        resolveWhereClause();
        return new UpdateQuery(targetTable, columnsToSet, setValues, renderedWhereSql, whereBindings);
    }

    private void resolveWhereClause() {
        if (namedWhereSql == null) {
            verifyAllBindingsUsed(java.util.Set.of());
            return;
        }
        PreparedWhereClause prepared = sqlFragmentValidator.prepareNamed(namedWhereSql, whereNamedValues, fromScope);
        this.renderedWhereSql = prepared.renderedSql();
        this.whereBindings = prepared.orderedBindings();
        verifyAllBindingsUsed(prepared.usedNames());
    }

    private void verifyAllBindingsUsed(java.util.Set<String> usedNames) {
        for (String key : whereNamedValues.keySet()) {
            if (!usedNames.contains(key)) {
                throw new Sdp4jValidationException(
                        "Bound parameter '" + key + "' does not appear in the WHERE clause");
            }
        }
    }

    @Override
    public int execute() {
        RenderContext ctx = new RenderContext();
        toQuery().render(ctx);
        return queryExecutor.executeUpdate(ctx.sql(), ctx.bindings());
    }

    private void extractNonNullNonPrimaryKeyColumns(T entity) {
        this.columnsToSet = new ArrayList<>();
        this.setValues = new ArrayList<>();
        for (Map.Entry<String, Field> entry : entityDescriptor.fieldByColumn().entrySet()) {
            String columnName = entry.getKey();
            ColumnMetadata columnMetadata = entityDescriptor.columnMetadata(columnName);
            if (columnMetadata != null && columnMetadata.isPrimaryKey()) continue;
            Object value = readField(entry.getValue(), entity);
            if (value == null) continue;
            columnsToSet.add(columnName);
            setValues.add(value);
        }
    }

    private Object readField(Field field, T entity) {
        try {
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new Sq4jException("Failed to read field '" + field.getName()
                    + "' on " + entityClass.getName(), e);
        }
    }
}

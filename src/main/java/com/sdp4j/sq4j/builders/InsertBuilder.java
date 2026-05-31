package com.sdp4j.sq4j.builders;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.core.exception.Sq4jException;
import com.sdp4j.sq4j.steps.InsertExecuteStep;
import com.sdp4j.sq4j.steps.InsertValuesStep;
import com.sdp4j.sq4j.executors.QueryExecutor;
import com.sdp4j.sq4j.metadata.EntityDescriptor;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;
import com.sdp4j.sq4j.queryinternals.InsertQuery;
import com.sdp4j.sq4j.queryinternals.TableRef;
import com.sdp4j.sq4j.renderers.RenderContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InsertBuilder<T> implements InsertValuesStep<T>, InsertExecuteStep {

    private final QueryExecutor queryExecutor;
    private final Class<T> entityClass;
    private final EntityDescriptor entityDescriptor;
    private final List<String> allColumnsInDeclarationOrder;

    private List<Object> singleRowValues;
    private List<List<Object>> bindingsPerRow;
    private boolean batchMode;

    public InsertBuilder(EntityMetadataCache entityMetadataCache, QueryExecutor queryExecutor,
                         Class<T> entityClass) {
        this.queryExecutor = queryExecutor;
        this.entityClass = entityClass;
        this.entityDescriptor = entityMetadataCache.metadataFor(entityClass);
        this.allColumnsInDeclarationOrder = new ArrayList<>(entityDescriptor.fieldByColumn().keySet());
    }

    @Override
    public InsertExecuteStep value(T entity) {
        if (entity == null) {
            throw new Sdp4jValidationException("Cannot insert a null entity");
        }
        this.singleRowValues = readEveryFieldInOrder(entity);
        return this;
    }

    @Override
    public InsertExecuteStep values(List<T> entities) {
        if (entities == null || entities.isEmpty()) {
            throw new Sdp4jValidationException("values(List) requires a non-empty list of entities");
        }
        if (entities.contains(null)) {
            throw new Sdp4jValidationException("values(List) cannot contain null entries");
        }
        this.batchMode = true;
        this.bindingsPerRow = new ArrayList<>(entities.size());
        for (T entity : entities) {
            bindingsPerRow.add(readEveryFieldInOrder(entity));
        }
        return this;
    }

    @Override
    public InsertQuery toQuery() {
        if (batchMode) {
            throw new UnsupportedOperationException(
                    "toQuery() is not available for batch insert. Use execute() instead.");
        }
        if (singleRowValues == null) {
            throw new IllegalStateException("value(entity) must be called before toQuery()");
        }
        return new InsertQuery(
                new TableRef(entityDescriptor.tableName()),
                allColumnsInDeclarationOrder,
                singleRowValues);
    }

    @Override
    public int execute() {
        if (batchMode) {
            return queryExecutor.executeBatch(renderBatchSql(), bindingsPerRow);
        }
        RenderContext ctx = new RenderContext();
        toQuery().render(ctx);
        return queryExecutor.executeUpdate(ctx.sql(), ctx.bindings());
    }

    private String renderBatchSql() {
        InsertQuery skeleton = new InsertQuery(
                new TableRef(entityDescriptor.tableName()),
                allColumnsInDeclarationOrder,
                placeholderValues(allColumnsInDeclarationOrder.size()));
        RenderContext ctx = new RenderContext();
        skeleton.render(ctx);
        return ctx.sql();
    }

    private List<Object> placeholderValues(int size) {
        List<Object> placeholders = new ArrayList<>(size);
        for (int i = 0; i < size; i++) placeholders.add(null);
        return placeholders;
    }

    private List<Object> readEveryFieldInOrder(T entity) {
        List<Object> values = new ArrayList<>(allColumnsInDeclarationOrder.size());
        for (Map.Entry<String, Field> entry : entityDescriptor.fieldByColumn().entrySet()) {
            values.add(readField(entry.getValue(), entity));
        }
        return values;
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

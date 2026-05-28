package com.sdp4j.sq4j.builder;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.core.exception.Sq4jException;
import com.sdp4j.sq4j.dsl.InsertExecuteStep;
import com.sdp4j.sq4j.dsl.InsertValuesStep;
import com.sdp4j.sq4j.execute.QueryExecutor;
import com.sdp4j.sq4j.metadata.EntityDescriptor;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;
import com.sdp4j.sq4j.query.InsertQuery;
import com.sdp4j.sq4j.query.TableRef;
import com.sdp4j.sq4j.render.RenderContext;
import com.sdp4j.sq4j.render.SqlDialect;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InsertBuilder<T> implements InsertValuesStep<T>, InsertExecuteStep {

    private final QueryExecutor queryExecutor;
    private final SqlDialect dialect;
    private final Class<T> entityClass;
    private final EntityDescriptor entityDescriptor;

    private List<String> columnsToInsert;
    private List<Object> valuesToBind;
    private List<List<Object>> bindingsPerRow;
    private boolean batchMode;

    public InsertBuilder(EntityMetadataCache entityMetadataCache, QueryExecutor queryExecutor, SqlDialect dialect,
                         Class<T> entityClass) {
        this.queryExecutor = queryExecutor;
        this.dialect = dialect;
        this.entityClass = entityClass;
        this.entityDescriptor = entityMetadataCache.metadataFor(entityClass);
    }

    @Override
    public InsertExecuteStep value(T entity) {
        if (entity == null) {
            throw new Sdp4jValidationException("Cannot insert a null entity");
        }
        extractNonNullColumnsAndValues(entity);
        if (columnsToInsert.isEmpty()) {
            throw new Sdp4jValidationException(
                    "Cannot insert into '" + entityDescriptor.tableName()
                            + "': every field on the supplied " + entityClass.getSimpleName()
                            + " instance is null");
        }
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
        this.columnsToInsert = extractColumnSignature(entities.getFirst());
        if (columnsToInsert.isEmpty()) {
            throw new Sdp4jValidationException(
                    "Cannot insert into '" + entityDescriptor.tableName()
                            + "': every field on the first entity is null");
        }
        this.bindingsPerRow = new ArrayList<>(entities.size());
        for (int i = 0; i < entities.size(); i++) {
            bindingsPerRow.add(bindingsForRow(entities.get(i), columnsToInsert, i));
        }
        return this;
    }

    @Override
    public InsertQuery toQuery() {
        if (batchMode) {
            throw new UnsupportedOperationException(
                    "toQuery() is not available for batch insert. Use execute() instead.");
        }
        if (columnsToInsert == null) {
            throw new IllegalStateException("value(entity) must be called before toQuery()");
        }
        return new InsertQuery(
                new TableRef(entityDescriptor.tableName()),
                columnsToInsert,
                valuesToBind);
    }

    @Override
    public int execute() {
        if (batchMode) {
            String sql = renderBatchSql();
            return queryExecutor.executeBatch(sql, bindingsPerRow);
        }
        RenderContext ctx = new RenderContext(dialect);
        toQuery().render(ctx);
        return queryExecutor.executeUpdate(ctx.sql(), ctx.bindings());
    }

    private String renderBatchSql() {
        RenderContext ctx = new RenderContext(dialect);
        InsertQuery skeleton = new InsertQuery(
                new TableRef(entityDescriptor.tableName()),
                columnsToInsert,
                placeholderValues(columnsToInsert.size()));
        skeleton.render(ctx);
        return ctx.sql();
    }

    private List<Object> placeholderValues(int size) {
        List<Object> placeholders = new ArrayList<>(size);
        for (int i = 0; i < size; i++) placeholders.add(null);
        return placeholders;
    }

    private void extractNonNullColumnsAndValues(T entity) {
        this.columnsToInsert = new ArrayList<>();
        this.valuesToBind = new ArrayList<>();
        for (Map.Entry<String, Field> entry : entityDescriptor.fieldByColumn().entrySet()) {
            Object value = readField(entry.getValue(), entity);
            if (value == null) continue;
            columnsToInsert.add(entry.getKey());
            valuesToBind.add(value);
        }
    }

    private List<String> extractColumnSignature(T entity) {
        List<String> signature = new ArrayList<>();
        for (Map.Entry<String, Field> entry : entityDescriptor.fieldByColumn().entrySet()) {
            if (readField(entry.getValue(), entity) != null) {
                signature.add(entry.getKey());
            }
        }
        return signature;
    }

    private List<Object> bindingsForRow(T entity, List<String> signature, int rowIndex) {
        Set<String> entityNonNullColumns = new LinkedHashSet<>(extractColumnSignature(entity));
        if (!entityNonNullColumns.equals(new LinkedHashSet<>(signature))) {
            throw new Sdp4jValidationException(
                    "Batch insert column signature mismatch at row " + rowIndex
                            + ": expected " + signature + " but entity has " + entityNonNullColumns
                            + ". Group entities by non-null columns before batching.");
        }
        List<Object> values = new ArrayList<>(signature.size());
        for (String column : signature) {
            values.add(readField(entityDescriptor.fieldFor(column), entity));
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

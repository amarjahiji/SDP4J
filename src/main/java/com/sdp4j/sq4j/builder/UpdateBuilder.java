package com.sdp4j.sq4j.builder;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.core.exception.Sq4jException;
import com.sdp4j.sm4j.metadata.ColumnMetadata;
import com.sdp4j.sq4j.dsl.UpdateExecuteStep;
import com.sdp4j.sq4j.dsl.UpdateSetStep;
import com.sdp4j.sq4j.dsl.UpdateWhereStep;
import com.sdp4j.sq4j.execute.QueryExecutor;
import com.sdp4j.sq4j.metadata.EntityDescriptor;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;
import com.sdp4j.sq4j.query.TableRef;
import com.sdp4j.sq4j.query.UpdateQuery;
import com.sdp4j.sq4j.render.RenderContext;
import com.sdp4j.sq4j.render.SqlDialect;
import com.sdp4j.sq4j.validation.FromScope;
import com.sdp4j.sq4j.validation.PreparedWhereClause;
import com.sdp4j.sq4j.validation.WhereClauseValidator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UpdateBuilder<T> implements UpdateSetStep<T>, UpdateWhereStep, UpdateExecuteStep {

    private final QueryExecutor queryExecutor;
    private final SqlDialect dialect;
    private final WhereClauseValidator sqlFragmentValidator = new WhereClauseValidator();

    private final Class<T> entityClass;
    private final EntityDescriptor entityDescriptor;
    private final TableRef targetTable;
    private final FromScope fromScope = new FromScope();

    private List<String> columnsToSet;
    private List<Object> setValues;
    private String renderedWhereSql;
    private List<Object> whereBindings = List.of();

    private boolean batchMode;
    private List<List<Object>> batchBindingsPerRow;
    private String batchPkColumn;

    public UpdateBuilder(EntityMetadataCache entityMetadataCache, QueryExecutor queryExecutor, SqlDialect dialect,
                         Class<T> entityClass, String alias) {
        this.queryExecutor = queryExecutor;
        this.dialect = dialect;
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
    public UpdateExecuteStep patches(List<T> patches) {
        if (patches == null || patches.isEmpty()) {
            throw new Sdp4jValidationException("patches(List) requires a non-empty list");
        }
        if (patches.contains(null)) {
            throw new Sdp4jValidationException("patches(List) cannot contain null entries");
        }
        this.batchMode = true;
        this.batchPkColumn = locateSinglePrimaryKeyColumn();
        this.columnsToSet = extractNonNullNonPkSignature(patches.getFirst());
        if (columnsToSet.isEmpty()) {
            throw new Sdp4jValidationException(
                    "First patch has no non-primary-key fields set; nothing to update");
        }
        this.batchBindingsPerRow = new ArrayList<>(patches.size());
        for (int i = 0; i < patches.size(); i++) {
            batchBindingsPerRow.add(bindingsForPatch(patches.get(i), columnsToSet, i));
        }
        return this;
    }

    @Override
    public UpdateExecuteStep where(String sql, Object... values) {
        PreparedWhereClause prepared = sqlFragmentValidator.prepare(sql, values, fromScope);
        this.renderedWhereSql = prepared.renderedSql();
        this.whereBindings = prepared.orderedBindings();
        return this;
    }

    @Override
    public UpdateQuery toQuery() {
        if (batchMode) {
            throw new UnsupportedOperationException(
                    "toQuery() is not available for batch update. Use execute() instead.");
        }
        if (columnsToSet == null) {
            throw new IllegalStateException("set(entity) must be called before toQuery()");
        }
        return new UpdateQuery(targetTable, columnsToSet, setValues, renderedWhereSql, whereBindings);
    }

    @Override
    public int execute() {
        if (batchMode) {
            String sql = renderBatchSql();
            return queryExecutor.executeBatch(sql, batchBindingsPerRow);
        }
        RenderContext ctx = new RenderContext(dialect);
        toQuery().render(ctx);
        return queryExecutor.executeUpdate(ctx.sql(), ctx.bindings());
    }

    private String renderBatchSql() {
        List<Object> placeholders = new ArrayList<>(columnsToSet.size());
        for (int i = 0; i < columnsToSet.size(); i++) placeholders.add(null);
        UpdateQuery skeleton = new UpdateQuery(
                targetTable,
                columnsToSet,
                placeholders,
                batchPkColumn + " = ?",
                List.of());
        RenderContext ctx = new RenderContext(dialect);
        skeleton.render(ctx);
        return ctx.sql();
    }

    private String locateSinglePrimaryKeyColumn() {
        List<String> pkColumns = new ArrayList<>();
        for (Map.Entry<String, Field> entry : entityDescriptor.fieldByColumn().entrySet()) {
            ColumnMetadata columnMetadata = entityDescriptor.columnMetadata(entry.getKey());
            if (columnMetadata != null && columnMetadata.isPrimaryKey()) {
                pkColumns.add(entry.getKey());
            }
        }
        if (pkColumns.size() != 1) {
            throw new Sdp4jValidationException(
                    "Batch patches require exactly one @PrimaryKey column on "
                            + entityClass.getSimpleName() + " — found " + pkColumns.size());
        }
        return pkColumns.getFirst();
    }

    private List<String> extractNonNullNonPkSignature(T entity) {
        List<String> signature = new ArrayList<>();
        for (Map.Entry<String, Field> entry : entityDescriptor.fieldByColumn().entrySet()) {
            String columnName = entry.getKey();
            if (columnName.equals(batchPkColumn)) continue;
            if (readField(entry.getValue(), entity) != null) {
                signature.add(columnName);
            }
        }
        return signature;
    }

    private List<Object> bindingsForPatch(T patch, List<String> signature, int patchIndex) {
        Object pkValue = readField(entityDescriptor.fieldFor(batchPkColumn), patch);
        if (pkValue == null) {
            throw new Sdp4jValidationException(
                    "Patch at index " + patchIndex + " has no primary-key value set on column '"
                            + batchPkColumn + "'");
        }
        Set<String> patchSignature = new LinkedHashSet<>(extractNonNullNonPkSignature(patch));
        if (!patchSignature.equals(new LinkedHashSet<>(signature))) {
            throw new Sdp4jValidationException(
                    "Batch update signature mismatch at row " + patchIndex
                            + ": expected " + signature + " but patch has " + patchSignature
                            + ". Group patches by non-null columns before batching.");
        }
        List<Object> rowBindings = new ArrayList<>(signature.size() + 1);
        for (String column : signature) {
            rowBindings.add(readField(entityDescriptor.fieldFor(column), patch));
        }
        rowBindings.add(pkValue);
        return rowBindings;
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

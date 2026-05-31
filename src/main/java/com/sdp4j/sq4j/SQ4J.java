package com.sdp4j.sq4j;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sq4j.builders.DeleteBuilder;
import com.sdp4j.sq4j.builders.InsertBuilder;
import com.sdp4j.sq4j.builders.SelectBuilder;
import com.sdp4j.sq4j.builders.UpdateBuilder;
import com.sdp4j.sq4j.steps.DeleteWhereStep;
import com.sdp4j.sq4j.steps.InsertValuesStep;
import com.sdp4j.sq4j.steps.SelectFromStep;
import com.sdp4j.sq4j.steps.UpdateSetStep;
import com.sdp4j.sq4j.executors.QueryExecutor;
import com.sdp4j.sq4j.metadata.DtoMetadataCache;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

public class SQ4J {

    private final EntityMetadataCache entityMetadataCache;
    private final DtoMetadataCache dtoMetadataCache;
    private final QueryExecutor queryExecutor;

    public SQ4J(DataSource dataSource) {
        this.entityMetadataCache = new EntityMetadataCache();
        this.dtoMetadataCache = new DtoMetadataCache();
        this.queryExecutor = new QueryExecutor(dataSource);
    }

    public SelectFromStep select(String... columnNames) {
        if (columnNames == null || columnNames.length == 0) {
            throw new Sdp4jValidationException("select(String...) requires at least one column name");
        }
        List<String> rawColumns = Arrays.asList(columnNames);
        return new SelectBuilder(entityMetadataCache, dtoMetadataCache, queryExecutor, rawColumns);
    }

    public <T> InsertValuesStep<T> insertInto(Class<T> entityClass) {
        return new InsertBuilder<>(entityMetadataCache, queryExecutor, entityClass);
    }

    public <T> UpdateSetStep<T> update(Class<T> entityClass) {
        return new UpdateBuilder<>(entityMetadataCache, queryExecutor, entityClass, null);
    }

    public <T> UpdateSetStep<T> update(Class<T> entityClass, String alias) {
        return new UpdateBuilder<>(entityMetadataCache, queryExecutor, entityClass, alias);
    }

    public DeleteWhereStep deleteFrom(Class<?> entityClass) {
        return new DeleteBuilder(entityMetadataCache, queryExecutor, entityClass, null);
    }

    public DeleteWhereStep deleteFrom(Class<?> entityClass, String alias) {
        return new DeleteBuilder(entityMetadataCache, queryExecutor, entityClass, alias);
    }

    public EntityMetadataCache entityMetadataCache() {
        return entityMetadataCache;
    }

    public DtoMetadataCache dtoMetadataCache() {
        return dtoMetadataCache;
    }
}

package com.sdp4j.sq4j;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sq4j.builder.DeleteBuilder;
import com.sdp4j.sq4j.builder.InsertBuilder;
import com.sdp4j.sq4j.builder.SelectBuilder;
import com.sdp4j.sq4j.builder.UpdateBuilder;
import com.sdp4j.sq4j.dsl.DeleteWhereStep;
import com.sdp4j.sq4j.dsl.InsertValuesStep;
import com.sdp4j.sq4j.dsl.SelectFromStep;
import com.sdp4j.sq4j.dsl.UpdateSetStep;
import com.sdp4j.sq4j.execute.QueryExecutor;
import com.sdp4j.sq4j.metadata.DtoMetadataCache;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;
import com.sdp4j.sq4j.render.PostgresDialect;
import com.sdp4j.sq4j.render.SqlDialect;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

public class SQ4J {

    private final EntityMetadataCache entityMetadataCache;
    private final DtoMetadataCache dtoMetadataCache;
    private final QueryExecutor queryExecutor;
    private final SqlDialect dialect;

    public SQ4J(DataSource dataSource) {
        this.entityMetadataCache = new EntityMetadataCache();
        this.dtoMetadataCache = new DtoMetadataCache();
        this.queryExecutor = new QueryExecutor(dataSource);
        this.dialect = new PostgresDialect();
    }

    public SelectFromStep select(String... columnNames) {
        if (columnNames == null || columnNames.length == 0) {
            throw new Sdp4jValidationException("select(String...) requires at least one column name");
        }
        List<String> rawColumns = Arrays.asList(columnNames);
        return new SelectBuilder(entityMetadataCache, dtoMetadataCache, queryExecutor, dialect, rawColumns);
    }

    public <T> InsertValuesStep<T> insertInto(Class<T> entityClass) {
        return new InsertBuilder<>(entityMetadataCache, queryExecutor, dialect, entityClass);
    }

    public <T> UpdateSetStep<T> update(Class<T> entityClass) {
        return new UpdateBuilder<>(entityMetadataCache, queryExecutor, dialect, entityClass, null);
    }

    public <T> UpdateSetStep<T> update(Class<T> entityClass, String alias) {
        return new UpdateBuilder<>(entityMetadataCache, queryExecutor, dialect, entityClass, alias);
    }

    public DeleteWhereStep deleteFrom(Class<?> entityClass) {
        return new DeleteBuilder(entityMetadataCache, queryExecutor, dialect, entityClass, null);
    }

    public DeleteWhereStep deleteFrom(Class<?> entityClass, String alias) {
        return new DeleteBuilder(entityMetadataCache, queryExecutor, dialect, entityClass, alias);
    }

    public EntityMetadataCache entityMetadataCache() {
        return entityMetadataCache;
    }

    public DtoMetadataCache dtoMetadataCache() {
        return dtoMetadataCache;
    }

    public SqlDialect dialect() {
        return dialect;
    }
}

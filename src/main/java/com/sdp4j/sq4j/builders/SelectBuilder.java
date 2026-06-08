package com.sdp4j.sq4j.builders;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sm4j.annotations.Table;
import com.sdp4j.sq4j.steps.SelectFetchStep;
import com.sdp4j.sq4j.steps.SelectFromStep;
import com.sdp4j.sq4j.steps.SelectJoinStep;
import com.sdp4j.sq4j.steps.SelectBindStep;
import com.sdp4j.sq4j.steps.SelectLimitStep;
import com.sdp4j.sq4j.steps.SelectOnStep;
import com.sdp4j.sq4j.steps.SelectOrderByStep;
import com.sdp4j.sq4j.steps.SelectWhereStep;
import com.sdp4j.sq4j.mappers.DtoRowMapper;
import com.sdp4j.sq4j.mappers.EntityRowMapper;
import com.sdp4j.sq4j.executors.QueryExecutor;
import com.sdp4j.sq4j.mappers.RowMapper;
import com.sdp4j.sq4j.metadata.DtoDescriptor;
import com.sdp4j.sq4j.metadata.DtoMetadataCache;
import com.sdp4j.sq4j.metadata.EntityDescriptor;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;
import com.sdp4j.sq4j.queryinternals.FieldRef;
import com.sdp4j.sq4j.queryinternals.JoinSpec;
import com.sdp4j.sq4j.enums.JoinType;
import com.sdp4j.sq4j.queryinternals.SelectQuery;
import com.sdp4j.sq4j.queryinternals.TableRef;
import com.sdp4j.sq4j.renderers.RenderContext;
import com.sdp4j.sq4j.validations.FromScope;
import com.sdp4j.sq4j.validations.PreparedWhereClause;
import com.sdp4j.sq4j.validations.WhereClauseValidator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SelectBuilder
        implements SelectFromStep, SelectJoinStep, SelectOnStep, SelectWhereStep,
        SelectBindStep, SelectOrderByStep, SelectLimitStep, SelectFetchStep {

    private static final String STAR_TOKEN = "*";

    private final EntityMetadataCache entityMetadataCache;
    private final DtoMetadataCache dtoMetadataCache;
    private final QueryExecutor queryExecutor;
    private final WhereClauseValidator sqlFragmentValidator = new WhereClauseValidator();

    private final List<String> rawColumnProjection;
    private final List<FieldRef> projectedFields = new ArrayList<>();
    private final List<StagedJoin> stagedJoins = new ArrayList<>();
    private final FromScope fromScope = new FromScope();

    private record StagedJoin(JoinType type, TableRef table, String rawOnSql) {
    }

    private boolean distinct;
    private TableRef fromTable;
    private JoinType pendingJoinType;
    private TableRef pendingJoinTable;
    private EntityDescriptor pendingJoinDescriptor;
    private String pendingJoinQualifier;
    private String namedWhereSql;
    private final Map<String, Object> whereNamedValues = new LinkedHashMap<>();
    private String renderedWhereSql;
    private List<Object> whereBindings = List.of();
    private String orderBySql;
    private Integer limit;
    private Integer offset;

    public SelectBuilder(EntityMetadataCache entityMetadataCache, DtoMetadataCache dtoMetadataCache,
                         QueryExecutor queryExecutor,
                         List<String> rawColumnProjection) {
        this.entityMetadataCache = entityMetadataCache;
        this.dtoMetadataCache = dtoMetadataCache;
        this.queryExecutor = queryExecutor;
        this.rawColumnProjection = new ArrayList<>(rawColumnProjection);
    }

    @Override
    public SelectJoinStep from(Class<?> entityClass) {
        return from(entityClass, null);
    }

    @Override
    public SelectJoinStep from(Class<?> entityClass, String alias) {
        EntityDescriptor descriptor = entityMetadataCache.metadataFor(entityClass);
        this.fromTable = new TableRef(descriptor.tableName(), alias);
        String qualifier = alias != null ? alias : descriptor.tableName();
        fromScope.register(qualifier, descriptor);
        return this;
    }

    @Override
    public SelectOnStep innerJoin(Class<?> entityClass, String alias) {
        return stageJoin(JoinType.INNER, entityClass, alias);
    }

    @Override
    public SelectOnStep leftJoin(Class<?> entityClass, String alias) {
        return stageJoin(JoinType.LEFT, entityClass, alias);
    }

    @Override
    public SelectOnStep rightJoin(Class<?> entityClass, String alias) {
        return stageJoin(JoinType.RIGHT, entityClass, alias);
    }

    @Override
    public SelectOnStep fullOuterJoin(Class<?> entityClass, String alias) {
        return stageJoin(JoinType.FULL_OUTER, entityClass, alias);
    }

    @Override
    public SelectJoinStep on(String sql) {
        if (pendingJoinType == null) {
            throw new IllegalStateException("on(...) called without a preceding join");
        }
        fromScope.register(pendingJoinQualifier, pendingJoinDescriptor);
        stagedJoins.add(new StagedJoin(pendingJoinType, pendingJoinTable, sql));
        clearPendingJoin();
        return this;
    }

    @Override
    public SelectJoinStep distinct() {
        this.distinct = true;
        return this;
    }

    @Override
    public SelectBindStep where(String sql) {
        this.namedWhereSql = sql;
        return this;
    }

    @Override
    public SelectBindStep set(String name, Object value) {
        if (name == null || !name.startsWith(":")) {
            throw new Sdp4jValidationException(
                    "Parameter name must start with ':' (e.g. \":id\"), got: " + name);
        }
        whereNamedValues.put(name, value);
        return this;
    }

    @Override
    public SelectLimitStep orderBy(String sql) {
        this.orderBySql = sql;
        return this;
    }

    @Override
    public SelectFetchStep limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public SelectFetchStep limit(int limit, int offset) {
        this.limit = limit;
        this.offset = offset;
        return this;
    }

    @Override
    public SelectQuery toQuery() {
        validateAndPromoteProjection();
        Set<String> usedNames = new HashSet<>();
        List<JoinSpec> joins = resolveJoins(usedNames);
        resolveWhereClause(usedNames);
        verifyAllBindingsUsed(usedNames);
        return new SelectQuery(distinct, projectedFields, fromTable, joins,
                renderedWhereSql, whereBindings, orderBySql, limit, offset);
    }

    private List<JoinSpec> resolveJoins(Set<String> usedNames) {
        List<JoinSpec> joins = new ArrayList<>(stagedJoins.size());
        for (StagedJoin staged : stagedJoins) {
            PreparedWhereClause prepared =
                    sqlFragmentValidator.prepareNamed(staged.rawOnSql(), whereNamedValues, fromScope);
            joins.add(new JoinSpec(staged.type(), staged.table(),
                    prepared.renderedSql(), prepared.orderedBindings()));
            usedNames.addAll(prepared.usedNames());
        }
        return joins;
    }

    private void resolveWhereClause(Set<String> usedNames) {
        if (namedWhereSql == null) return;
        PreparedWhereClause prepared = sqlFragmentValidator.prepareNamed(namedWhereSql, whereNamedValues, fromScope);
        this.renderedWhereSql = prepared.renderedSql();
        this.whereBindings = prepared.orderedBindings();
        usedNames.addAll(prepared.usedNames());
    }

    private void verifyAllBindingsUsed(Set<String> usedNames) {
        for (String key : whereNamedValues.keySet()) {
            if (!usedNames.contains(key)) {
                throw new Sdp4jValidationException(
                        "Bound parameter '" + key + "' does not appear in any WHERE or ON clause");
            }
        }
    }

    @Override
    public <T> List<T> mapTo(Class<T> type) {
        RowMapper<T> rowMapper = pickRowMapper(type);
        RenderContext renderContext = new RenderContext();
        toQuery().render(renderContext);
        return queryExecutor.fetch(renderContext.sql(), renderContext.bindings(), rowMapper);
    }

    private <T> RowMapper<T> pickRowMapper(Class<T> type) {
        if (type.isAnnotationPresent(Table.class)) {
            EntityDescriptor entityDescriptor = entityMetadataCache.metadataFor(type);
            return new EntityRowMapper<>(type, entityDescriptor);
        }
        DtoDescriptor dtoDescriptor = dtoMetadataCache.descriptorFor(type);
        return new DtoRowMapper<>(type, dtoDescriptor);
    }

    @Override
    public <T> Optional<T> mapToOne(Class<T> type) {
        List<T> rows = mapTo(type);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    private SelectOnStep stageJoin(JoinType joinType, Class<?> entityClass, String alias) {
        EntityDescriptor descriptor = entityMetadataCache.metadataFor(entityClass);
        String qualifier = alias != null ? alias : descriptor.tableName();
        this.pendingJoinType = joinType;
        this.pendingJoinTable = new TableRef(descriptor.tableName(), alias);
        this.pendingJoinDescriptor = descriptor;
        this.pendingJoinQualifier = qualifier;
        return this;
    }

    private void clearPendingJoin() {
        this.pendingJoinType = null;
        this.pendingJoinTable = null;
        this.pendingJoinDescriptor = null;
        this.pendingJoinQualifier = null;
    }

    private void validateAndPromoteProjection() {
        if (!projectedFields.isEmpty()) return;
        if (isStarProjection(rawColumnProjection)) {
            projectedFields.add(FieldRef.STAR);
            return;
        }
        for (String rawColumn : rawColumnProjection) {
            String expression = rawColumn.trim();
            String alias = null;
            // Split an optional trailing "AS <alias>" (case-insensitive). The alias
            // becomes the result-set column label, which drives DTO field matching.
            String[] parts = expression.split("(?i)\\s+as\\s+", 2);
            if (parts.length == 2) {
                expression = parts[0].trim();
                alias = parts[1].trim();
                validateAlias(alias);
            }
            int dotIndex = expression.indexOf('.');
            if (dotIndex >= 0) {
                promoteQualifiedProjection(expression, dotIndex, alias);
            } else {
                promoteBareProjection(expression, alias);
            }
        }
    }

    private void promoteQualifiedProjection(String expression, int dotIndex, String alias) {
        String qualifier = expression.substring(0, dotIndex);
        String column = expression.substring(dotIndex + 1);
        if (!fromScope.hasQualifier(qualifier)) {
            throw new Sdp4jValidationException(
                    "Unknown table qualifier '" + qualifier + "' in select. Known qualifiers: "
                            + fromScope.qualifiers());
        }
        EntityDescriptor descriptor = fromScope.descriptorFor(qualifier);
        if (!descriptor.hasColumn(column)) {
            throw new Sdp4jValidationException(
                    "Unknown column '" + column + "' on table '" + descriptor.tableName() + "'");
        }
        projectedFields.add(new FieldRef(qualifier, column, alias));
    }

    private void promoteBareProjection(String column, String alias) {
        List<String> matchingQualifiers = fromScope.qualifiersWhereColumnExists(column);
        if (matchingQualifiers.isEmpty()) {
            throw new Sdp4jValidationException(
                    "Unknown column '" + column + "' — not found on any table in scope: "
                            + fromScope.qualifiers());
        }
        if (matchingQualifiers.size() > 1) {
            throw new Sdp4jValidationException(
                    "Ambiguous column '" + column + "' — exists on tables " + matchingQualifiers
                            + ". Qualify it (e.g. " + matchingQualifiers.getFirst() + "." + column + ").");
        }
        if (fromScope.isMultiTable()) {
            projectedFields.add(new FieldRef(matchingQualifiers.getFirst(), column, alias));
        } else {
            projectedFields.add(new FieldRef(null, column, alias));
        }
    }

    private void validateAlias(String alias) {
        if (!alias.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new Sdp4jValidationException(
                    "Invalid column alias '" + alias + "' — must be a plain identifier");
        }
    }

    private boolean isStarProjection(List<String> rawColumns) {
        return rawColumns.size() == 1 && STAR_TOKEN.equals(rawColumns.getFirst());
    }
}

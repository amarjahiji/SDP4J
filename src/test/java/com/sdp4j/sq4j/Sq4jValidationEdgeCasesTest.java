package com.sdp4j.sq4j;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sm4j.annotations.PrimaryKey;
import com.sdp4j.sm4j.annotations.Table;
import com.sdp4j.sq4j.builders.InsertBuilder;
import com.sdp4j.sq4j.builders.SelectBuilder;
import com.sdp4j.sq4j.builders.UpdateBuilder;
import com.sdp4j.sq4j.executors.QueryExecutor;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;
import com.sdp4j.sq4j.queryinternals.SelectQuery;
import com.sdp4j.sq4j.renderers.RenderContext;
import com.sdp4j.sq4j.validations.FromScope;
import com.sdp4j.sq4j.validations.PreparedWhereClause;
import com.sdp4j.sq4j.validations.WhereClauseValidator;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sq4jValidationEdgeCasesTest {

    private final SQ4J sq4j = new SQ4J(null);

    @Test
    void sq4jRejectsInvalidEntryArguments() {
        assertThrows(Sdp4jValidationException.class, sq4j::select);
        assertThrows(Sdp4jValidationException.class, () -> sq4j.select((String[]) null));
        assertThrows(Sdp4jValidationException.class, () -> sq4j.insertInto(NotTable.class));
        assertThrows(Sdp4jValidationException.class, () -> sq4j.update(NotTable.class));
        assertThrows(Sdp4jValidationException.class, () -> sq4j.deleteFrom(NotTable.class));
    }

    @Test
    void insertBuilderRejectsInvalidStateAndBatchToQuery() {
        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.insertInto(UserEntity.class).value(null));
        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.insertInto(UserEntity.class).values(List.of()));
        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.insertInto(UserEntity.class).values(Arrays.asList(new UserEntity("u-1", "Ada"), null)));
        assertThrows(IllegalStateException.class,
                () -> new InsertBuilder<>(new EntityMetadataCache(), new QueryExecutor(null), UserEntity.class).toQuery());
        assertThrows(UnsupportedOperationException.class,
                () -> sq4j.insertInto(UserEntity.class).values(List.of(new UserEntity("u-1", "Ada"))).toQuery());
    }

    @Test
    void updateBuilderRejectsNullEmptyAndInvalidBindingState() {
        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.update(UserEntity.class).set(null));
        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.update(UserEntity.class).set(new UserEntity("only-id", null)));
        assertThrows(IllegalStateException.class,
                () -> new UpdateBuilder<>(new EntityMetadataCache(), new QueryExecutor(null), UserEntity.class, null).toQuery());
        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.update(UserEntity.class)
                        .set(new UserEntity(null, "Ada"))
                        .where("id = :id")
                        .set("id", "u-1"));
        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.update(UserEntity.class)
                        .set(new UserEntity(null, "Ada"))
                        .where("id = :id")
                        .set(":unused", "u-1")
                        .toQuery());
    }

    @Test
    void whereValidatorHandlesStringsCastsFunctionsCollectionsAndMalformedInput() {
        EntityMetadataCache cache = new EntityMetadataCache();
        FromScope scope = new FromScope();
        scope.register("u", cache.metadataFor(UserEntity.class));
        WhereClauseValidator validator = new WhereClauseValidator();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(":ids", List.of("u-1", "u-2"));
        values.put(":name", "Ada");

        PreparedWhereClause prepared = validator.prepareNamed(
                "LOWER(u.name) = LOWER(:name) AND u.id::uuid IN :ids AND u.name = ':not_a_param'",
                values,
                scope);

        assertEquals("LOWER(u.name) = LOWER(?) AND u.id::uuid IN (?, ?) AND u.name = ':not_a_param'",
                prepared.renderedSql());
        assertEquals(List.of("Ada", "u-1", "u-2"), prepared.orderedBindings());
        assertTrue(prepared.usedNames().containsAll(List.of(":name", ":ids")));

        assertThrows(Sdp4jValidationException.class,
                () -> validator.prepareNamed("id IN :ids", Map.of(":ids", List.of()), scope));
        assertThrows(Sdp4jValidationException.class,
                () -> validator.prepareNamed("id = :", Map.of(), scope));
        assertThrows(Sdp4jValidationException.class,
                () -> validator.prepareNamed("name = 'unterminated", Map.of(), scope));
        assertThrows(Sdp4jValidationException.class,
                () -> validator.prepareNamed("id = :missing", Map.of(), scope));
    }

    @Test
    void selectBuilderRejectsAmbiguousColumnsDuplicateQualifiersAndMissingJoinContext() {
        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.select("id")
                        .from(UserEntity.class, "u")
                        .innerJoin(RoleEntity.class, "r").on("u.id = r.id")
                        .toQuery());

        assertThrows(IllegalStateException.class,
                () -> sq4j.select("*")
                        .from(UserEntity.class, "u")
                        .innerJoin(RoleEntity.class, "u").on("u.id = u.id")
                        .toQuery());

        SelectBuilder builder = new SelectBuilder(new EntityMetadataCache(), null, new QueryExecutor(null), List.of("*"));
        assertThrows(IllegalStateException.class, () -> builder.on("id = id"));
    }

    @Test
    void selectDistinctAndStarProjectionRenderCleanly() {
        SelectQuery query = sq4j.select("*")
                .from(UserEntity.class)
                .distinct()
                .where("name = :name")
                .set(":name", "Ada")
                .toQuery();
        RenderContext ctx = new RenderContext();

        query.render(ctx);

        assertEquals("SELECT DISTINCT * FROM users WHERE name = ?", ctx.sql());
        assertEquals(List.of("Ada"), ctx.bindings());
    }

    @Table(name = "users", mapToSnakeCase = true)
    static class UserEntity {
        @PrimaryKey
        private String id;
        private String name;

        UserEntity() {
        }

        UserEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    @Table(name = "roles", mapToSnakeCase = true)
    static class RoleEntity {
        @PrimaryKey
        private String id;
        private String name;
    }

    static class NotTable {
        private String id;
    }
}

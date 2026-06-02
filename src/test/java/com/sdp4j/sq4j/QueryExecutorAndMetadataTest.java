package com.sdp4j.sq4j;

import com.sdp4j.core.exception.Sdp4jQueryException;
import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sm4j.annotations.PrimaryKey;
import com.sdp4j.sm4j.annotations.Table;
import com.sdp4j.sq4j.executors.QueryExecutor;
import com.sdp4j.sq4j.metadata.DtoDescriptor;
import com.sdp4j.sq4j.metadata.DtoMetadataCache;
import com.sdp4j.sq4j.metadata.EntityDescriptor;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;
import com.sdp4j.sq4j.queryinternals.FieldRef;
import com.sdp4j.sq4j.queryinternals.JoinSpec;
import com.sdp4j.sq4j.queryinternals.SelectQuery;
import com.sdp4j.sq4j.queryinternals.TableRef;
import com.sdp4j.sq4j.enums.JoinType;
import com.sdp4j.testsupport.JdbcProxySupport;
import com.sdp4j.testsupport.JdbcProxySupport.RecordingDataSource;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryExecutorAndMetadataTest {

    @Test
    void queryExecutorFetchBindsValuesAndMapsEveryRow() {
        RecordingDataSource dataSource = JdbcProxySupport.recordingDataSource()
                .withRows(List.of(
                        linkedMap("name", "Ada"),
                        linkedMap("name", "Grace")));
        QueryExecutor executor = new QueryExecutor(dataSource);

        List<String> names = executor.fetch("SELECT name FROM users WHERE active = ?",
                List.of(true),
                rs -> rs.getString("name"));

        assertEquals(List.of("Ada", "Grace"), names);
        assertEquals("SELECT name FROM users WHERE active = ?",
                dataSource.preparedStatements().getFirst().sql());
        assertEquals(Map.of(1, true), dataSource.preparedStatements().getFirst().boundValues());
        assertTrue(dataSource.preparedStatements().getFirst().closed());
    }

    @Test
    void queryExecutorExecutesUpdatesAndBatchesWithBindings() {
        RecordingDataSource dataSource = JdbcProxySupport.recordingDataSource()
                .withUpdateCount(3)
                .withBatchCounts(1, 1, -2);
        QueryExecutor executor = new QueryExecutor(dataSource);

        int updated = executor.executeUpdate("UPDATE users SET active = ? WHERE tenant_id = ?",
                List.of(false, 42L));
        int batchTotal = executor.executeBatch("INSERT INTO users (id) VALUES (?)",
                List.of(List.of("u-1"), List.of("u-2"), List.of("u-3")));

        assertEquals(3, updated);
        assertEquals(Map.of(1, false, 2, 42L), dataSource.preparedStatements().get(0).boundValues());
        assertEquals(2, batchTotal);
        assertEquals(List.of(Map.of(1, "u-1"), Map.of(1, "u-2"), Map.of(1, "u-3")),
                dataSource.preparedStatements().get(1).batchBindings());
    }

    @Test
    void queryExecutorReturnsZeroForEmptyBatchAndWrapsSqlFailures() {
        RecordingDataSource dataSource = JdbcProxySupport.recordingDataSource();
        QueryExecutor executor = new QueryExecutor(dataSource);

        assertEquals(0, executor.executeBatch("INSERT INTO users (id) VALUES (?)", List.of()));
        assertTrue(dataSource.preparedStatements().isEmpty());

        QueryExecutor failingExecutor = new QueryExecutor(
                JdbcProxySupport.recordingDataSource().failOnPrepare(new SQLException("boom")));
        assertThrows(Sdp4jQueryException.class,
                () -> failingExecutor.executeUpdate("UPDATE users SET active = ?", List.of(true)));
    }

    @Test
    void entityAndDtoMetadataCachesReuseDescriptorsAndExposeFields() {
        EntityMetadataCache entityCache = new EntityMetadataCache();
        EntityDescriptor first = entityCache.metadataFor(UserEntity.class);
        EntityDescriptor second = entityCache.metadataFor(UserEntity.class);

        assertSame(first, second);
        assertEquals("users", first.tableName());
        assertTrue(first.hasColumn("first_name"));
        assertNotNull(first.fieldFor("first_name"));
        assertNotNull(first.columnMetadata("id"));
        assertThrows(Sdp4jValidationException.class, () -> entityCache.metadataFor(NotAnEntity.class));

        DtoMetadataCache dtoCache = new DtoMetadataCache();
        DtoDescriptor dto = dtoCache.descriptorFor(UserSummary.class);
        assertSame(dto, dtoCache.descriptorFor(UserSummary.class));
        assertNotNull(dto.fieldFor("first_name"));
    }

    @Test
    void queryInternalsExposeEffectiveNamesAndDefensiveLists() {
        TableRef aliased = new TableRef("users", "u");
        TableRef plain = new TableRef("users");
        FieldRef qualified = new FieldRef("u", "id");
        JoinSpec join = new JoinSpec(JoinType.LEFT, new TableRef("roles", "r"), "u.role_id = r.id", List.of("x"));
        SelectQuery query = new SelectQuery(false, List.of(qualified), aliased, List.of(join),
                "u.id = ?", List.of("u-1"), null, null, null);

        assertEquals("u", aliased.effectiveName());
        assertEquals("users", plain.effectiveName());
        assertTrue(qualified.isQualified());
        assertEquals("id", qualified.column());
        assertThrows(UnsupportedOperationException.class, () -> query.fields().add(FieldRef.STAR));
        assertThrows(UnsupportedOperationException.class, () -> query.joins().add(join));
        assertThrows(UnsupportedOperationException.class, () -> query.whereBindings().add("u-2"));
        assertThrows(UnsupportedOperationException.class, () -> join.onBindings().add("y"));
    }

    private static Map<String, Object> linkedMap(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }

    @Table(name = "users", mapToSnakeCase = true)
    static class UserEntity {
        @PrimaryKey
        private String id;
        private String firstName;
    }

    static class NotAnEntity {
        private String id;
    }

    static class UserSummary {
        private String firstName;
    }
}

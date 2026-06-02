package com.sdp4j.sq4j;

import com.sdp4j.core.exception.Sps4jException;
import com.sdp4j.sm4j.annotations.PrimaryKey;
import com.sdp4j.sm4j.annotations.Table;
import com.sdp4j.sps4j.SPS4J;
import com.sdp4j.sq4j.mappers.DtoRowMapper;
import com.sdp4j.sq4j.mappers.EntityRowMapper;
import com.sdp4j.sq4j.metadata.DtoMetadataCache;
import com.sdp4j.sq4j.metadata.EntityMetadataCache;
import com.sdp4j.testsupport.JdbcProxySupport;
import com.sdp4j.testsupport.JdbcProxySupport.PreparedStatementCall;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MappingAndSps4jTest {

    @Test
    void mapsEntityFieldsByColumnLabelsAndIgnoresUnknownColumns() throws Exception {
        ResultSet rs = JdbcProxySupport.resultSet(Map.of(
                "id", "u-1",
                "first_name", "Ada",
                "active", true,
                "ignored_column", "ignored"));

        EntityRowMapper<UserEntity> mapper = new EntityRowMapper<>(
                UserEntity.class,
                new EntityMetadataCache().metadataFor(UserEntity.class));

        UserEntity mapped = mapper.map(rs);

        assertEquals("u-1", mapped.id);
        assertEquals("Ada", mapped.firstName);
        assertEquals(true, mapped.active);
    }

    @Test
    void mapsDtoFieldsUsingSnakeCaseColumnLabels() throws Exception {
        ResultSet rs = JdbcProxySupport.resultSet(Map.of(
                "user_id", "u-1",
                "role_name", "admin"));

        DtoRowMapper<UserRoleDto> mapper = new DtoRowMapper<>(
                UserRoleDto.class,
                new DtoMetadataCache().descriptorFor(UserRoleDto.class));

        UserRoleDto mapped = mapper.map(rs);

        assertEquals("u-1", mapped.userId);
        assertEquals("admin", mapped.roleName);
    }

    @Test
    void sps4jRewritesNamedParametersAndBindsEveryOccurrence() {
        PreparedStatementCall[] captured = new PreparedStatementCall[1];
        Connection connection = JdbcProxySupport.connectionRecording(captured);

        SPS4J statement = new SPS4J(connection,
                "SELECT ':literal' AS literal, id::uuid FROM users WHERE id = :id OR parent_id = :id AND status = :status");
        statement.set(":id", "u-1").set(":status", "active");

        assertEquals("SELECT ':literal' AS literal, id::uuid FROM users WHERE id = ? OR parent_id = ? AND status = ?",
                captured[0].sql());
        assertEquals(Map.of(1, "u-1", 2, "u-1", 3, "active"), captured[0].boundValues());
    }

    @Test
    void sps4jSupportsNullBindingAndRejectsUnknownParameters() {
        PreparedStatementCall[] captured = new PreparedStatementCall[1];
        Connection connection = JdbcProxySupport.connectionRecording(captured);

        SPS4J statement = new SPS4J(connection, "SELECT * FROM users WHERE deleted_at IS :deleted_at");
        statement.setNull(":deleted_at", Types.TIMESTAMP);

        assertNull(captured[0].boundValues().get(1));
        assertThrows(Sps4jException.class, () -> statement.set(":missing", "x"));
        assertThrows(Sps4jException.class, () -> new SPS4J(connection, " "));
    }

    @Table(name = "users", mapToSnakeCase = true)
    public static class UserEntity {
        @PrimaryKey
        private String id;

        private String firstName;
        private Boolean active;

        public UserEntity() {
        }
    }

    public static class UserRoleDto {
        private String userId;
        private String roleName;

        public UserRoleDto() {
        }
    }
}

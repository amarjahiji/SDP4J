package com.sdp4j.sq4j;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sm4j.annotations.DefaultFalse;
import com.sdp4j.sm4j.annotations.ForeignKey;
import com.sdp4j.sm4j.annotations.Length;
import com.sdp4j.sm4j.annotations.NotNull;
import com.sdp4j.sm4j.annotations.PrimaryKey;
import com.sdp4j.sm4j.annotations.Table;
import com.sdp4j.sq4j.queryinternals.DeleteQuery;
import com.sdp4j.sq4j.queryinternals.InsertQuery;
import com.sdp4j.sq4j.queryinternals.SelectQuery;
import com.sdp4j.sq4j.queryinternals.UpdateQuery;
import com.sdp4j.sq4j.renderers.RenderContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class Sq4jQueryBuilderTest {

    private final SQ4J sq4j = new SQ4J(null);

    @Test
    void rendersSelectWithJoinNamedBindingsCollectionExpansionAndPaging() {
        SelectQuery query = sq4j.select("u.id", "u.first_name", "r.name")
                .from(UserEntity.class, "u")
                .innerJoin(RoleEntity.class, "r").on("u.role_id = r.id AND r.name = :role")
                .where("u.active = :active AND u.id IN :ids")
                .set(":role", "admin")
                .set(":active", true)
                .set(":ids", List.of("u-1", "u-2"))
                .orderBy("u.first_name ASC")
                .limit(10, 5)
                .toQuery();

        RenderContext ctx = render(query);

        assertEquals("SELECT u.id, u.first_name, r.name FROM users u "
                + "INNER JOIN roles r ON u.role_id = r.id AND r.name = ? "
                + "WHERE u.active = ? AND u.id IN (?, ?) "
                + "ORDER BY u.first_name ASC LIMIT 10 OFFSET 5", ctx.sql());
        assertEquals(List.of("admin", true, "u-1", "u-2"), ctx.bindings());
    }

    @Test
    void validatesProjectionWhereFragmentsAndBindingsBeforeExecution() {
        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.select("missing").from(UserEntity.class).toQuery());

        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.select("*").from(UserEntity.class)
                        .where("missing = :value")
                        .set(":value", "x")
                        .toQuery());

        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.select("*").from(UserEntity.class)
                        .where("id = :id")
                        .set(":unused", "x")
                        .toQuery());

        assertThrows(Sdp4jValidationException.class,
                () -> sq4j.select("*").from(UserEntity.class)
                        .where("id = ?")
                        .toQuery());
    }

    @Test
    void rendersInsertFromEntityDeclarationOrder() {
        UserEntity user = new UserEntity("u-1", "Ada", "ada@example.test", 7L, true,
                new BigDecimal("99.50"), "r-1");

        InsertQuery query = sq4j.insertInto(UserEntity.class).value(user).toQuery();
        RenderContext ctx = render(query);

        assertEquals("INSERT INTO users (id, first_name, email_address, tenant_id, active, balance, role_id) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", ctx.sql());
        assertEquals(List.of("u-1", "Ada", "ada@example.test", 7L, true,
                new BigDecimal("99.50"), "r-1"), ctx.bindings());
    }

    @Test
    void rendersUpdateSkippingPrimaryKeyAndNullPatchFields() {
        UserEntity patch = new UserEntity("ignored-id", "Grace", null, null, true, null, null);

        UpdateQuery query = sq4j.update(UserEntity.class, "u")
                .set(patch)
                .where("u.id = :id")
                .set(":id", "u-2")
                .toQuery();
        RenderContext ctx = render(query);

        assertEquals("UPDATE users u SET first_name = ?, active = ? WHERE u.id = ?", ctx.sql());
        assertEquals(List.of("Grace", true, "u-2"), ctx.bindings());
    }

    @Test
    void rendersDeleteWithValidatedWhereClause() {
        DeleteQuery query = sq4j.deleteFrom(UserEntity.class)
                .where("active = :active")
                .set(":active", false)
                .toQuery();
        RenderContext ctx = render(query);

        assertEquals("DELETE FROM users WHERE active = ?", ctx.sql());
        assertEquals(List.of(false), ctx.bindings());
    }

    private RenderContext render(com.sdp4j.sq4j.queryinternals.Query query) {
        RenderContext ctx = new RenderContext();
        query.render(ctx);
        return ctx;
    }

    @Table(name = "roles", mapToSnakeCase = true)
    static class RoleEntity {
        @PrimaryKey
        private String id;

        private String name;
    }

    @Table(name = "users", mapToSnakeCase = true)
    static class UserEntity {
        @PrimaryKey
        private String id;

        @NotNull
        @Length(120)
        private String firstName;

        private String emailAddress;
        private Long tenantId;

        @DefaultFalse
        private Boolean active;

        private BigDecimal balance;

        @ForeignKey(mapsTo = RoleEntity.class)
        private String roleId;

        UserEntity() {
        }

        UserEntity(String id, String firstName, String emailAddress, Long tenantId,
                   Boolean active, BigDecimal balance, String roleId) {
            this.id = id;
            this.firstName = firstName;
            this.emailAddress = emailAddress;
            this.tenantId = tenantId;
            this.active = active;
            this.balance = balance;
            this.roleId = roleId;
        }
    }
}

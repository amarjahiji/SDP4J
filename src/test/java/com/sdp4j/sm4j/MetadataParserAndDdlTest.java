package com.sdp4j.sm4j;

import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.sm4j.annotations.DefaultFalse;
import com.sdp4j.sm4j.annotations.DefaultInt;
import com.sdp4j.sm4j.annotations.DefaultNumeric;
import com.sdp4j.sm4j.annotations.ForeignKey;
import com.sdp4j.sm4j.annotations.Index;
import com.sdp4j.sm4j.annotations.Length;
import com.sdp4j.sm4j.annotations.NotNull;
import com.sdp4j.sm4j.annotations.PrimaryKey;
import com.sdp4j.sm4j.annotations.Table;
import com.sdp4j.sm4j.annotations.UniqueKeysConstraint;
import com.sdp4j.sm4j.ddlgenerators.CommonDdlsGenerator;
import com.sdp4j.sm4j.ddlgenerators.CreateDdlsGenerator;
import com.sdp4j.sm4j.enums.OnDelete;
import com.sdp4j.sm4j.metadata.ColumnMetadata;
import com.sdp4j.sm4j.metadata.TableMetadata;
import com.sdp4j.sm4j.parsers.MetadataParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetadataParserAndDdlTest {

    private final MetadataParser parser = new MetadataParser();

    @Test
    void parsesAnnotatedEntityIntoStableTableMetadata() {
        TableMetadata metadata = parser.parse(AccountUser.class);
        Map<String, ColumnMetadata> columns = metadata.getColumnMetadata().stream()
                .collect(Collectors.toMap(ColumnMetadata::getName, Function.identity()));

        assertEquals("account_users", metadata.getName());
        assertTrue(metadata.isSnakeCase());
        assertArrayEquals(new String[]{"tenant_id", "email_address"},
                metadata.getUniqueKeysConstraints().get(0));

        assertEquals("VARCHAR(36)", columns.get("id").getType());
        assertTrue(columns.get("id").isPrimaryKey());
        assertFalse(columns.get("id").isNullable());

        assertEquals("VARCHAR(120)", columns.get("email_address").getType());
        assertFalse(columns.get("email_address").isNullable());

        assertEquals(0, columns.get("login_count").getDefaultIntValue());
        assertTrue(columns.get("active").isDefaultFalse());
        assertEquals("12.3400", columns.get("balance").getDefaultNumericValue());

        ColumnMetadata roleId = columns.get("role_id");
        assertEquals("roles", roleId.getForeignKeyTableReference());
        assertEquals("id", roleId.getForeignKeyReferencedColumn());
        assertEquals("CASCADE", roleId.getForeignKeyOnDeleteAction());
        assertTrue(roleId.isIndex());
    }

    @Test
    void rendersCreateTableDdlWithPrimaryKeyDefaultsAndUniqueConstraints() {
        TableMetadata metadata = parser.parse(AccountUser.class);

        String ddl = new CreateDdlsGenerator().generateTableWithoutFkAndIdx(metadata);

        assertTrue(ddl.contains("CREATE TABLE IF NOT EXISTS account_users"));
        assertTrue(ddl.contains("id VARCHAR(36) NOT NULL"));
        assertTrue(ddl.contains("login_count INT DEFAULT 0"));
        assertTrue(ddl.contains("active BOOLEAN DEFAULT FALSE"));
        assertTrue(ddl.contains("balance NUMERIC DEFAULT 12.3400"));
        assertTrue(ddl.contains("PRIMARY KEY (id)"));
        assertTrue(ddl.contains("CONSTRAINT uq_account_users_tenant_id_email_address UNIQUE (tenant_id, email_address)"));
    }

    @Test
    void rejectsInvalidAnnotationCombinationsEarly() {
        assertThrows(Sdp4jValidationException.class, () -> parser.parse(InvalidLengthEntity.class));
        assertThrows(Sdp4jValidationException.class, () -> parser.parse(InvalidDefaultEntity.class));
        assertThrows(Sdp4jValidationException.class, () -> parser.parse(MissingTableAnnotation.class));
    }

    @Test
    void validatesSqlIdentifiersBeforeDdlHelpersUseThem() {
        CommonDdlsGenerator generator = new CommonDdlsGenerator();

        assertEquals("idx_users_email", generator.buildIndexName("users", "email"));
        assertThrows(Sdp4jValidationException.class,
                () -> generator.validateSqlIdentifiers("test", "users", "bad-name"));
    }

    @Table(name = "roles", mapToSnakeCase = true)
    static class Role {
        @PrimaryKey
        private String id;
        private String name;
    }

    @Table(name = "account_users", mapToSnakeCase = true)
    @UniqueKeysConstraint(keys = {"tenantId", "emailAddress"})
    static class AccountUser {
        @PrimaryKey
        @Length(36)
        private String id;

        private Long tenantId;

        @NotNull
        @Length(120)
        private String emailAddress;

        @DefaultInt(0)
        private Integer loginCount;

        @DefaultFalse
        private Boolean active;

        @DefaultNumeric("12.3400")
        private BigDecimal balance;

        @Index
        @ForeignKey(mapsTo = Role.class, action = OnDelete.CASCADE)
        private String roleId;
    }

    @Table
    static class InvalidLengthEntity {
        @PrimaryKey
        private String id;

        @Length(10)
        private Integer notAString;
    }

    @Table
    static class InvalidDefaultEntity {
        @PrimaryKey
        private String id;

        @DefaultInt(1)
        private String notAnInteger;
    }

    static class MissingTableAnnotation {
        @PrimaryKey
        private String id;
    }
}

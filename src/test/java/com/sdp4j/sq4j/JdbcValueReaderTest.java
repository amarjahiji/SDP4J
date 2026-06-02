package com.sdp4j.sq4j;

import com.sdp4j.sq4j.mappers.JdbcValueReader;
import com.sdp4j.testsupport.JdbcProxySupport;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JdbcValueReaderTest {

    @Test
    void readsSupportedJdbcValuesIntoLibraryJavaTypes() throws Exception {
        UUID uuid = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 1);
        LocalDateTime dateTime = LocalDateTime.of(2026, 6, 1, 12, 30);
        ResultSet rs = JdbcProxySupport.resultSet(row(
                "string_value", "Ada",
                "long_value", 7L,
                "int_value", 5,
                "double_value", 3.5d,
                "float_value", 1.25f,
                "boolean_value", true,
                "decimal_value", new BigDecimal("12.34"),
                "date_value", date,
                "datetime_value", dateTime,
                "instant_value", dateTime,
                "uuid_value", uuid.toString(),
                "object_value", "raw"));

        assertEquals("Ada", JdbcValueReader.read(rs, 1, String.class));
        assertEquals(7L, JdbcValueReader.read(rs, 2, Long.class));
        assertEquals(5, JdbcValueReader.read(rs, 3, Integer.class));
        assertEquals(3.5d, JdbcValueReader.read(rs, 4, Double.class));
        assertEquals(1.25f, JdbcValueReader.read(rs, 5, Float.class));
        assertEquals(true, JdbcValueReader.read(rs, 6, Boolean.class));
        assertEquals(new BigDecimal("12.34"), JdbcValueReader.read(rs, 7, BigDecimal.class));
        assertEquals(date, JdbcValueReader.read(rs, 8, LocalDate.class));
        assertEquals(dateTime, JdbcValueReader.read(rs, 9, LocalDateTime.class));
        assertEquals(Instant.parse("2026-06-01T12:30:00Z"), JdbcValueReader.read(rs, 10, Instant.class));
        assertEquals(uuid, JdbcValueReader.read(rs, 11, UUID.class));
        assertEquals("raw", JdbcValueReader.read(rs, 12, Object.class));
    }

    @Test
    void returnsNullForNullablePrimitiveWrapperReadsWhenJdbcValueWasNull() throws Exception {
        ResultSet rs = JdbcProxySupport.resultSet(row(
                "long_value", null,
                "int_value", null,
                "double_value", null,
                "float_value", null,
                "boolean_value", null,
                "instant_value", null,
                "uuid_value", null));

        assertNull(JdbcValueReader.read(rs, 1, Long.class));
        assertNull(JdbcValueReader.read(rs, 2, Integer.class));
        assertNull(JdbcValueReader.read(rs, 3, Double.class));
        assertNull(JdbcValueReader.read(rs, 4, Float.class));
        assertNull(JdbcValueReader.read(rs, 5, Boolean.class));
        assertNull(JdbcValueReader.read(rs, 6, Instant.class));
        assertNull(JdbcValueReader.read(rs, 7, UUID.class));
    }

    private static Map<String, Object> row(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}

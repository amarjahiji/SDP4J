package com.sdp4j.sq4j.execute;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

public final class JdbcValueReader {

    private JdbcValueReader() {
    }

    public static Object read(ResultSet rs, int columnIndex, Class<?> javaType) throws SQLException {
        if (javaType == String.class) return rs.getString(columnIndex);
        if (javaType == Long.class || javaType == long.class) {
            long value = rs.getLong(columnIndex);
            return rs.wasNull() ? null : value;
        }
        if (javaType == Integer.class || javaType == int.class) {
            int value = rs.getInt(columnIndex);
            return rs.wasNull() ? null : value;
        }
        if (javaType == Double.class || javaType == double.class) {
            double value = rs.getDouble(columnIndex);
            return rs.wasNull() ? null : value;
        }
        if (javaType == Float.class || javaType == float.class) {
            float value = rs.getFloat(columnIndex);
            return rs.wasNull() ? null : value;
        }
        if (javaType == Boolean.class || javaType == boolean.class) {
            boolean value = rs.getBoolean(columnIndex);
            return rs.wasNull() ? null : value;
        }
        if (javaType == LocalDate.class) return rs.getObject(columnIndex, LocalDate.class);
        if (javaType == LocalDateTime.class) return rs.getObject(columnIndex, LocalDateTime.class);
        if (javaType == Instant.class) {
            LocalDateTime asLocal = rs.getObject(columnIndex, LocalDateTime.class);
            return asLocal == null ? null : asLocal.toInstant(ZoneOffset.UTC);
        }
        if (javaType == UUID.class) {
            Object raw = rs.getObject(columnIndex);
            if (raw == null) return null;
            return raw instanceof UUID uuid ? uuid : UUID.fromString(raw.toString());
        }
        return rs.getObject(columnIndex);
    }
}

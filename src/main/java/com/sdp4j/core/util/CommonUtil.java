package com.sdp4j.core.util;

import java.sql.*;
import java.util.Collection;

public class CommonUtil {
    private CommonUtil() {
    }

    public static String toSnakeCase(String input) {
        if (!isValidString(input)) {
            return input;
        }

        StringBuilder result = new StringBuilder();
        char[] chars = input.trim().toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (Character.isUpperCase(c)) {
                if (i > 0 && chars[i - 1] != '_') {
                    result.append('_');
                }
                result.append(Character.toLowerCase(c));
            } else if (Character.isWhitespace(c) || c == '-') {
                if (!result.isEmpty() && result.charAt(result.length() - 1) != '_') {
                    result.append('_');
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    public static boolean isValidCollection(Collection<?> collection) {
        return collection != null && !collection.isEmpty();
    }

    public static boolean isValidString(String str) {
        return str != null && !str.isBlank();
    }

    public static boolean areValidStrings(String... str) {
        if (str == null || str.length == 0) {
            return false;
        }
        for (String s : str) {
            if (!isValidString(s)) {
                return false;
            }
        }
        return true;
    }

    public static boolean areValidSqlIdentifiers(String... identifier) {
        if (identifier == null || identifier.length == 0) {
            return false;
        }
        for (String s : identifier) {
            if (!isValidString(s)) {
                return false;
            }
            if (!s.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                return false;
            }
        }
        return true;
    }

    public static String getOrDefault(String value, String defaultValue) {
        if (isValidString(value)) {
            return value;
        }
        return defaultValue;
    }

    public static void close(ResultSet rs, PreparedStatement ps, Connection connection) throws SQLException {
        if (rs != null) {
            rs.close();
        }
        if (ps != null) {
            ps.close();
        }
        if (connection != null) {
            connection.close();
        }
    }

    public static void close(ResultSet rs, Statement st, Connection connection) throws SQLException {
        if (rs != null) {
            rs.close();
        }
        if (st != null) {
            st.close();
        }
        if (connection != null) {
            connection.close();
        }
    }
}
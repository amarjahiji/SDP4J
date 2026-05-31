package com.sdp4j.sps4j;

import com.sdp4j.core.exception.Sps4jException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SPS4J implements AutoCloseable {

    private final String parsedSql;
    private final Map<String, List<Integer>> parameterIndexes;
    private final PreparedStatement statement;

    public SPS4J(Connection connection, String namedSql) {
        if (namedSql == null || namedSql.isBlank()) {
            throw new Sps4jException("SQL query must not be null or blank");
        }
        StringBuilder rendered = new StringBuilder(namedSql.length());
        this.parameterIndexes = parse(namedSql, rendered);
        this.parsedSql = rendered.toString();
        try {
            this.statement = connection.prepareStatement(parsedSql);
        } catch (SQLException e) {
            throw new Sps4jException("Failed to prepare statement: " + parsedSql, e);
        }
    }

    public SPS4J set(String name, Object value) {
        for (int index : indexesFor(name)) {
            try {
                statement.setObject(index, value);
            } catch (SQLException e) {
                throw new Sps4jException("Failed to bind parameter '" + name + "'", e);
            }
        }
        return this;
    }

    public SPS4J set(int oneBasedIndex, Object value) {
        try {
            statement.setObject(oneBasedIndex, value);
        } catch (SQLException e) {
            throw new Sps4jException("Failed to bind parameter at index " + oneBasedIndex, e);
        }
        return this;
    }

    public SPS4J setNull(String name, int sqlType) {
        for (int index : indexesFor(name)) {
            try {
                statement.setNull(index, sqlType);
            } catch (SQLException e) {
                throw new Sps4jException("Failed to bind NULL parameter '" + name + "'", e);
            }
        }
        return this;
    }

    public SPS4J setNull(int oneBasedIndex, int sqlType) {
        try {
            statement.setNull(oneBasedIndex, sqlType);
        } catch (SQLException e) {
            throw new Sps4jException("Failed to bind NULL at index " + oneBasedIndex, e);
        }
        return this;
    }

    public void addBatch() {
        try {
            statement.addBatch();
        } catch (SQLException e) {
            throw new Sps4jException("Failed to add batch", e);
        }
    }

    public ResultSet executeQuery() {
        try {
            return statement.executeQuery();
        } catch (SQLException e) {
            throw new Sps4jException("Failed to execute query", e);
        }
    }

    public int executeUpdate() {
        try {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new Sps4jException("Failed to execute update", e);
        }
    }

    public boolean execute() {
        try {
            return statement.execute();
        } catch (SQLException e) {
            throw new Sps4jException("Failed to execute statement", e);
        }
    }

    public int[] executeBatch() {
        try {
            return statement.executeBatch();
        } catch (SQLException e) {
            throw new Sps4jException("Failed to execute batch", e);
        }
    }

    public PreparedStatement unwrap() {
        return statement;
    }

    @Override
    public void close() {
        if (statement == null) {
            return;
        }
        try {
            statement.close();
        } catch (SQLException e) {
            throw new Sps4jException("Failed to close statement", e);
        }
    }

    private List<Integer> indexesFor(String name) {
        if (name == null || !name.startsWith(":")) {
            throw new Sps4jException("Parameter name must start with ':' (e.g. \":id\"), got: " + name);
        }
        List<Integer> indexes = parameterIndexes.get(name);
        if (indexes == null) {
            throw new Sps4jException("Unknown parameter '" + name + "' for sql: " + parsedSql);
        }
        return indexes;
    }

    private static Map<String, List<Integer>> parse(String namedSql, StringBuilder rendered) {
        Map<String, List<Integer>> indexes = new HashMap<>();
        int parameterIndex = 1;
        int length = namedSql.length();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < length; i++) {
            char c = namedSql.charAt(i);

            if (inSingleQuote) {
                rendered.append(c);
                if (c == '\'') inSingleQuote = false;
                continue;
            }
            if (inDoubleQuote) {
                rendered.append(c);
                if (c == '"') inDoubleQuote = false;
                continue;
            }
            if (c == '\'') {
                inSingleQuote = true;
                rendered.append(c);
                continue;
            }
            if (c == '"') {
                inDoubleQuote = true;
                rendered.append(c);
                continue;
            }
            if (c == ':') {
                if (i + 1 < length && namedSql.charAt(i + 1) == ':') {
                    rendered.append("::");
                    i++;
                    continue;
                }
                if (i + 1 < length && Character.isJavaIdentifierStart(namedSql.charAt(i + 1))) {
                    int j = i + 1;
                    while (j < length && Character.isJavaIdentifierPart(namedSql.charAt(j))) {
                        j++;
                    }
                    String name = namedSql.substring(i, j);
                    indexes.computeIfAbsent(name, _ -> new ArrayList<>()).add(parameterIndex++);
                    rendered.append('?');
                    i = j - 1;
                    continue;
                }
            }
            rendered.append(c);
        }

        return indexes;
    }
}

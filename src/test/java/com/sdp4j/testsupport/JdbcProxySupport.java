package com.sdp4j.testsupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.sql.DataSource;

public final class JdbcProxySupport {

    private JdbcProxySupport() {
    }

    public record PreparedStatementCall(String sql, Map<Integer, Object> boundValues) {
    }

    public static ResultSet resultSet(Map<String, Object> row) {
        return singleRowResultSet(row);
    }

    public static ResultSet resultSet(List<Map<String, Object>> rows) {
        List<Map<String, Object>> orderedRows = rows.stream()
                .<Map<String, Object>>map(LinkedHashMap::new)
                .toList();
        List<String> labels = orderedRows.isEmpty()
                ? List.of()
                : List.copyOf(orderedRows.getFirst().keySet());
        ResultSetMetaData metaData = resultSetMetaData(labels);
        final int[] cursor = {-1};
        final Object[] lastValue = {null};
        InvocationHandler handler = (_, method, args) -> switch (method.getName()) {
            case "next" -> ++cursor[0] < orderedRows.size();
            case "getMetaData" -> metaData;
            case "getObject" -> {
                if (args.length == 2 && args[1] instanceof Class<?> type) {
                    Object value = valueAt(orderedRows.get(cursor[0]), labels, args);
                    lastValue[0] = value;
                    yield value == null ? null : type.cast(value);
                }
                Object value = valueAt(orderedRows.get(cursor[0]), labels, args);
                lastValue[0] = value;
                yield value;
            }
            case "getString" -> {
                Object value = valueAt(orderedRows.get(cursor[0]), labels, args);
                lastValue[0] = value;
                yield value == null ? null : value.toString();
            }
            case "getInt" -> {
                Object value = valueAt(orderedRows.get(cursor[0]), labels, args);
                lastValue[0] = value;
                yield value == null ? 0 : ((Number) value).intValue();
            }
            case "getLong" -> {
                Object value = valueAt(orderedRows.get(cursor[0]), labels, args);
                lastValue[0] = value;
                yield value == null ? 0L : ((Number) value).longValue();
            }
            case "getFloat" -> {
                Object value = valueAt(orderedRows.get(cursor[0]), labels, args);
                lastValue[0] = value;
                yield value == null ? 0f : ((Number) value).floatValue();
            }
            case "getDouble" -> {
                Object value = valueAt(orderedRows.get(cursor[0]), labels, args);
                lastValue[0] = value;
                yield value == null ? 0d : ((Number) value).doubleValue();
            }
            case "getBigDecimal" -> {
                Object value = valueAt(orderedRows.get(cursor[0]), labels, args);
                lastValue[0] = value;
                yield value;
            }
            case "getBoolean" -> {
                Object value = valueAt(orderedRows.get(cursor[0]), labels, args);
                lastValue[0] = value;
                yield value == null ? false : value;
            }
            case "wasNull" -> lastValue[0] == null;
            case "close" -> null;
            default -> defaultValue(method.getReturnType());
        };
        return (ResultSet) Proxy.newProxyInstance(
                JdbcProxySupport.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                handler);
    }

    public static ResultSet singleRowResultSet(Map<String, Object> row) {
        List<String> labels = List.copyOf(row.keySet());
        ResultSetMetaData metaData = resultSetMetaData(labels);
        final Object[] lastValue = {null};
        InvocationHandler handler = (_, method, args) -> switch (method.getName()) {
            case "getMetaData" -> metaData;
            case "getObject", "getBigDecimal" -> {
                Object value = valueAt(row, labels, args);
                lastValue[0] = value;
                yield value;
            }
            case "getString" -> {
                Object value = valueAt(row, labels, args);
                lastValue[0] = value;
                yield value == null ? null : value.toString();
            }
            case "getInt" -> {
                Number value = numberAt(row, labels, args);
                lastValue[0] = value;
                yield value == null ? 0 : value.intValue();
            }
            case "getLong" -> {
                Number value = numberAt(row, labels, args);
                lastValue[0] = value;
                yield value == null ? 0L : value.longValue();
            }
            case "getFloat" -> {
                Number value = numberAt(row, labels, args);
                lastValue[0] = value;
                yield value == null ? 0f : value.floatValue();
            }
            case "getDouble" -> {
                Number value = numberAt(row, labels, args);
                lastValue[0] = value;
                yield value == null ? 0d : value.doubleValue();
            }
            case "getBoolean" -> {
                Object value = valueAt(row, labels, args);
                lastValue[0] = value;
                yield value == null ? false : value;
            }
            case "wasNull" -> lastValue[0] == null;
            default -> defaultValue(method.getReturnType());
        };
        return (ResultSet) Proxy.newProxyInstance(
                JdbcProxySupport.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                handler);
    }

    public static RecordingDataSource recordingDataSource() {
        return new RecordingDataSource();
    }

    public static final class RecordingDataSource implements DataSource {
        private final List<RecordedPreparedStatement> preparedStatements = new ArrayList<>();
        private List<Map<String, Object>> nextRows = List.of();
        private int nextUpdateCount = 1;
        private int[] nextBatchCounts;
        private SQLException prepareFailure;

        public List<RecordedPreparedStatement> preparedStatements() {
            return preparedStatements;
        }

        public RecordingDataSource withRows(List<Map<String, Object>> rows) {
            this.nextRows = rows;
            return this;
        }

        public RecordingDataSource withUpdateCount(int updateCount) {
            this.nextUpdateCount = updateCount;
            return this;
        }

        public RecordingDataSource withBatchCounts(int... counts) {
            this.nextBatchCounts = counts;
            return this;
        }

        public RecordingDataSource failOnPrepare(SQLException failure) {
            this.prepareFailure = failure;
            return this;
        }

        @Override
        public Connection getConnection() {
            InvocationHandler handler = (_, method, args) -> switch (method.getName()) {
                case "prepareStatement" -> prepare((String) args[0]);
                case "createArrayOf" -> sqlArray(args[0], (Object[]) args[1]);
                case "getSchema" -> "public";
                case "close" -> null;
                default -> defaultValue(method.getReturnType());
            };
            return (Connection) Proxy.newProxyInstance(
                    JdbcProxySupport.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    handler);
        }

        @Override
        public Connection getConnection(String username, String password) {
            return getConnection();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) return iface.cast(this);
            throw new SQLException("Not a wrapper for " + iface.getName());
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public java.io.PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        private PreparedStatement prepare(String sql) throws SQLException {
            if (prepareFailure != null) throw prepareFailure;
            RecordedPreparedStatement recorded = new RecordedPreparedStatement(sql, nextRows, nextUpdateCount, nextBatchCounts);
            preparedStatements.add(recorded);
            return recorded.proxy();
        }
    }

    public static final class RecordedPreparedStatement {
        private final String sql;
        private final List<Map<String, Object>> rows;
        private final int updateCount;
        private final int[] batchCounts;
        private final Map<Integer, Object> boundValues = new LinkedHashMap<>();
        private final List<Map<Integer, Object>> batchBindings = new ArrayList<>();
        private boolean closed;

        private RecordedPreparedStatement(String sql, List<Map<String, Object>> rows, int updateCount, int[] batchCounts) {
            this.sql = sql;
            this.rows = rows;
            this.updateCount = updateCount;
            this.batchCounts = batchCounts;
        }

        public String sql() {
            return sql;
        }

        public Map<Integer, Object> boundValues() {
            return Collections.unmodifiableMap(boundValues);
        }

        public List<Map<Integer, Object>> batchBindings() {
            return Collections.unmodifiableList(batchBindings);
        }

        public boolean closed() {
            return closed;
        }

        private PreparedStatement proxy() {
            InvocationHandler handler = (_, method, args) -> switch (method.getName()) {
                case "setObject", "setString", "setArray" -> {
                    boundValues.put((Integer) args[0], args[1]);
                    yield null;
                }
                case "setNull" -> {
                    boundValues.put((Integer) args[0], null);
                    yield null;
                }
                case "executeQuery" -> resultSet(rows);
                case "executeUpdate" -> updateCount;
                case "execute" -> true;
                case "addBatch" -> {
                    batchBindings.add(new LinkedHashMap<>(boundValues));
                    yield null;
                }
                case "executeBatch" -> batchCounts == null
                        ? batchBindings.stream().mapToInt(_ -> 1).toArray()
                        : batchCounts;
                case "close" -> {
                    closed = true;
                    yield null;
                }
                default -> defaultValue(method.getReturnType());
            };
            return (PreparedStatement) Proxy.newProxyInstance(
                    JdbcProxySupport.class.getClassLoader(),
                    new Class<?>[]{PreparedStatement.class},
                    handler);
        }
    }

    private static Array sqlArray(Object typeName, Object[] values) {
        InvocationHandler handler = (_, method, _) -> switch (method.getName()) {
            case "getBaseTypeName" -> typeName;
            case "getArray" -> values;
            case "free" -> null;
            default -> defaultValue(method.getReturnType());
        };
        return (Array) Proxy.newProxyInstance(
                JdbcProxySupport.class.getClassLoader(),
                new Class<?>[]{Array.class},
                handler);
    }

    public static Connection connectionRecording(PreparedStatementCall[] capturedCall) {
        InvocationHandler handler = (_, method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
                String sql = (String) args[0];
                Map<Integer, Object> boundValues = new LinkedHashMap<>();
                capturedCall[0] = new PreparedStatementCall(sql, boundValues);
                return preparedStatement(boundValues);
            }
            if ("close".equals(method.getName())) {
                return null;
            }
            return defaultValue(method.getReturnType());
        };
        return (Connection) Proxy.newProxyInstance(
                JdbcProxySupport.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                handler);
    }

    private static PreparedStatement preparedStatement(Map<Integer, Object> boundValues) {
        InvocationHandler handler = (_, method, args) -> {
            if ("setObject".equals(method.getName())) {
                boundValues.put((Integer) args[0], args[1]);
                return null;
            }
            if ("setNull".equals(method.getName())) {
                boundValues.put((Integer) args[0], null);
                return null;
            }
            if ("close".equals(method.getName())) {
                return null;
            }
            return defaultValue(method.getReturnType());
        };
        return (PreparedStatement) Proxy.newProxyInstance(
                JdbcProxySupport.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                handler);
    }

    private static ResultSetMetaData resultSetMetaData(List<String> labels) {
        InvocationHandler handler = (_, method, args) -> switch (method.getName()) {
            case "getColumnCount" -> labels.size();
            case "getColumnLabel", "getColumnName" -> labels.get(((Integer) args[0]) - 1);
            default -> defaultValue(method.getReturnType());
        };
        return (ResultSetMetaData) Proxy.newProxyInstance(
                JdbcProxySupport.class.getClassLoader(),
                new Class<?>[]{ResultSetMetaData.class},
                handler);
    }

    private static Object valueAt(Map<String, Object> row, List<String> labels, Object[] args) throws SQLException {
        Object key = args[0];
        if (key instanceof Integer index) {
            return row.get(labels.get(index - 1));
        }
        if (key instanceof String label) {
            return row.get(label);
        }
        throw new SQLException("Unsupported ResultSet lookup key: " + key);
    }

    private static String stringAt(Map<String, Object> row, List<String> labels, Object[] args) throws SQLException {
        Object value = valueAt(row, labels, args);
        return value == null ? null : value.toString();
    }

    private static Number numberAt(Map<String, Object> row, List<String> labels, Object[] args) throws SQLException {
        Object value = valueAt(row, labels, args);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number;
        }
        throw new SQLException("Value is not numeric: " + value);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) return null;
        if (returnType == Boolean.TYPE) return false;
        if (returnType == Byte.TYPE) return (byte) 0;
        if (returnType == Short.TYPE) return (short) 0;
        if (returnType == Integer.TYPE) return 0;
        if (returnType == Long.TYPE) return 0L;
        if (returnType == Float.TYPE) return 0f;
        if (returnType == Double.TYPE) return 0d;
        return null;
    }
}

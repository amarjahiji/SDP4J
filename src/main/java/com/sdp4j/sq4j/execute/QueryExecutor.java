package com.sdp4j.sq4j.execute;

import com.sdp4j.core.exception.Sdp4jQueryException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QueryExecutor {

    private final DataSource dataSource;

    public QueryExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> List<T> fetch(String sql, List<Object> bindings, RowMapper<T> mapper) {
        List<T> results = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, bindings);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }
        } catch (SQLException e) {
            throw new Sdp4jQueryException("Failed to execute query: " + sql, e);
        }
        return results;
    }

    public int executeUpdate(String sql, List<Object> bindings) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, bindings);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new Sdp4jQueryException("Failed to execute statement: " + sql, e);
        }
    }

    public int executeBatch(String sql, List<List<Object>> bindingsPerRow) {
        if (bindingsPerRow.isEmpty()) {
            return 0;
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            for (List<Object> rowBindings : bindingsPerRow) {
                bind(ps, rowBindings);
                ps.addBatch();
            }
            int[] perRowCounts = ps.executeBatch();
            int totalAffected = 0;
            for (int count : perRowCounts) {
                if (count >= 0) totalAffected += count;
            }
            return totalAffected;
        } catch (SQLException e) {
            throw new Sdp4jQueryException("Failed to execute batch: " + sql, e);
        }
    }

    private void bind(PreparedStatement ps, List<Object> bindings) throws SQLException {
        for (int i = 0; i < bindings.size(); i++) {
            ps.setObject(i + 1, bindings.get(i));
        }
    }
}
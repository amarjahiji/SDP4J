package com.sdp4j.demo.runners;

import com.sdp4j.core.client.Sdp4jClient;
import com.sdp4j.demo.ExampleDatabase;
import com.sdp4j.sps4j.SPS4J;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

public class Sps4jDemo {

    private final Sdp4jClient client;

    public Sps4jDemo() {
        this.client = ExampleDatabase.client();
    }

    public static void main(String[] args) throws SQLException {
        Sps4jDemo demo = new Sps4jDemo();

        demo.case1_fetchByNamedParam();
        demo.case2_reuseSameParamTwice();
        demo.case3_updateReturningCount();
        demo.case4_batchInsert();
        demo.case5_bindExplicitNull();
        demo.case6_postgresCastIsNotAParam();
        demo.case7_rawStatementEscapeHatch();
    }

    private void case1_fetchByNamedParam() throws SQLException {
        Connection con = null;
        SPS4J ps = null;
        ResultSet rs = null;
        try {
            con = client.getDataSource().getConnection();
            ps = client.getSps4j()
                    .connection(con)
                    .sql("SELECT * FROM users WHERE id = :id")
                    .set(":id", UUID.randomUUID().toString());
            rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println("case1 first_name=" + rs.getString("first_name"));
            }
        } finally {
            client.closeResources(rs, con, ps);
        }
    }

    private void case2_reuseSameParamTwice() throws SQLException {
        Connection con = null;
        SPS4J ps = null;
        ResultSet rs = null;
        try {
            con = client.getDataSource().getConnection();
            ps = client.getSps4j()
                    .connection(con)
                    .sql("SELECT * FROM users WHERE first_name = :name OR last_name = :name")
                    .set(":name", "Alice");
            rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println("case2 id=" + rs.getString("id"));
            }
        } finally {
            client.closeResources(rs, con, ps);
        }
    }

    private void case3_updateReturningCount() throws SQLException {
        Connection con = null;
        SPS4J ps = null;
        try {
            con = client.getDataSource().getConnection();
            ps = client.getSps4j()
                    .connection(con)
                    .sql("UPDATE users SET first_name = :fn WHERE id = :id")
                    .set(":fn", "Alicia")
                    .set(":id", "some-existing-id");
            int affected = ps.executeUpdate();
            System.out.println("case3 updated=" + affected);
        } finally {
            client.closeResources(con, ps);
        }
    }

    private void case4_batchInsert() throws SQLException {
        Connection con = null;
        SPS4J ps = null;
        try {
            con = client.getDataSource().getConnection();
            ps = client.getSps4j()
                    .connection(con)
                    .sql("INSERT INTO users (id, first_name) VALUES (:id, :fn)");
            ps.set(":id", UUID.randomUUID().toString()).set(":fn", "Alice").addBatch();
            ps.set(":id", UUID.randomUUID().toString()).set(":fn", "Bob").addBatch();
            int[] counts = ps.executeBatch();
            System.out.println("case4 batchRows=" + counts.length);
        } finally {
            client.closeResources(con, ps);
        }
    }

    private void case5_bindExplicitNull() throws SQLException {
        Connection con = null;
        SPS4J ps = null;
        try {
            con = client.getDataSource().getConnection();
            ps = client.getSps4j()
                    .connection(con)
                    .sql("UPDATE users SET last_name = :ln WHERE id = :id")
                    .setNull(":ln", Types.VARCHAR)
                    .set(":id", "some-existing-id");
            int affected = ps.executeUpdate();
            System.out.println("case5 updated=" + affected);
        } finally {
            client.closeResources(con, ps);
        }
    }

    private void case6_postgresCastIsNotAParam() throws SQLException {
        Connection con = null;
        SPS4J ps = null;
        ResultSet rs = null;
        try {
            con = client.getDataSource().getConnection();
            ps = client.getSps4j()
                    .connection(con)
                    .sql("SELECT id::text AS id FROM users WHERE is_active = :active")
                    .set(":active", true);
            rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println("case6 id=" + rs.getString("id"));
            }
        } finally {
            client.closeResources(rs, con, ps);
        }
    }

    private void case7_rawStatementEscapeHatch() throws SQLException {
        Connection con = null;
        SPS4J ps = null;
        ResultSet rs = null;
        try {
            con = client.getDataSource().getConnection();
            ps = client.getSps4j()
                    .connection(con)
                    .sql("SELECT * FROM users WHERE id = :id")
                    .set(":id", "some-existing-id");
            ps.unwrap().setFetchSize(50);
            rs = ps.executeQuery();
            System.out.println("case7 hasRow=" + rs.next());
        } finally {
            client.closeResources(rs, con, ps);
        }
    }
}

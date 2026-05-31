package com.sdp4j.core.client;

import com.sdp4j.core.exception.Sdp4jConfigurationException;
import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.sm4j.SM4J;
import com.sdp4j.sps4j.SPS4J;
import com.sdp4j.sps4j.steps.ConnectionStep;
import com.sdp4j.sps4j.steps.InitStep;
import com.sdp4j.sq4j.SQ4J;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;

public class Sdp4jClient {

    private final DataSource dataSource;
    private final String packageName;
    private final SM4J sm4j;
    private final SQ4J sq4j;

    public Sdp4jClient(String url, String username, String password, String packageName) {
        this.dataSource = generateDataSource(url, username, password);
        this.packageName = packageName;
        this.sm4j = new SM4J(this.dataSource, this.packageName);
        this.sq4j = new SQ4J(this.dataSource);
    }

    public Sdp4jClient(String packageName, DataSource dataSource) {
        this.dataSource = dataSource;
        this.packageName = packageName;
        this.sm4j = new SM4J(this.dataSource, this.packageName);
        this.sq4j = new SQ4J(this.dataSource);
    }

    private DataSource generateDataSource(String url, String username, String password) {
        if (!CommonUtil.isValidString(url) || !CommonUtil.isValidString(username)) {
            throw new Sdp4jConfigurationException("URL and username are required to initialize SDP4J client");
        }
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setMinimumIdle(5);
            config.setMaximumPoolSize(15);
            config.setConnectionTimeout(5000);
            config.setIdleTimeout(300000);
            config.setMaxLifetime(1800000);
            config.setLeakDetectionThreshold(60000);
            return new HikariDataSource(config);
        } catch (RuntimeException e) {
            throw new Sdp4jConfigurationException("Failed to configure datasource for SDP4J client", e);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public String getPackageName() {
        return packageName;
    }

    public SM4J getSm4j() {
        return sm4j;
    }

    public SQ4J getSq4j() {
        return sq4j;
    }

    public ConnectionStep getSps4j() {
        return new InitStep();
    }

    public void closeResources(ResultSet resultSet, Connection connection, SPS4J... statements) {
        closeQuietly(resultSet);
        if (statements != null) {
            for (SPS4J statement : statements) {
                closeQuietly(statement);
            }
        }
        closeQuietly(connection);
    }

    public void closeResources(Connection connection, SPS4J... statements) {
        closeResources(null, connection, statements);
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource == null) {
            return;
        }
        try {
            resource.close();
        } catch (Exception _) {
        }
    }
}
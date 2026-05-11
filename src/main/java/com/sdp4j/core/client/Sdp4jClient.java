package com.sdp4j.core.client;

import com.sdp4j.core.exception.Sdp4jConfigurationException;
import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.sm4j.services.Migration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class Sdp4jClient {

    private final DataSource dataSource;
    private final String packageName;
    private final Migration migration;

    public Sdp4jClient(String url, String username, String password, String packageName) {
        this.dataSource = generateDataSource(url, username, password);
        this.packageName = packageName;
        this.migration = new Migration(this.dataSource, this.packageName);
    }

    public Sdp4jClient(String packageName, DataSource dataSource) {
        this.dataSource = dataSource;
        this.packageName = packageName;
        this.migration = new Migration(this.dataSource, this.packageName);
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

    public Migration getMigration() {
        return migration;
    }
}

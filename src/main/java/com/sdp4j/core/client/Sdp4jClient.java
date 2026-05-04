package com.sdp4j.core.client;

import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.simplemigration.services.Migration;
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
            throw new RuntimeException("URL and username are required on MigrationClient for migration");
        }
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

package com.sdp4j.core.client;

import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.smigration.services.Migration;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class Sdp4jClient {

    private final DataSource dataSource;
    private final String packageName;
    private final Migration migration;

    /**
     * Constructs an Sdp4jClient by creating a JDBC-backed DataSource from the provided
     * connection credentials and initializing the migration service for the given package.
     *
     * @param url         the JDBC connection URL for the target database
     * @param username    the database username
     * @param password    the database password (may be null if the database allows passwordless connections)
     * @param packageName the package identifier used by the migration logic
     * @throws RuntimeException if the provided URL or username is invalid and a DataSource cannot be created
     */
    public Sdp4jClient(String url, String username, String password, String packageName) {
        this.dataSource = generateDataSource(url, username, password);
        this.packageName = packageName;
        this.migration = new Migration(this.dataSource, this.packageName);
    }

    /**
     * Create an Sdp4jClient backed by the given DataSource for the specified package.
     *
     * @param packageName the package identifier to use for migration operations
     * @param dataSource  the JDBC DataSource the client will use for database access
     */
    public Sdp4jClient(String packageName, DataSource dataSource) {
        this.dataSource = dataSource;
        this.packageName = packageName;
        this.migration = new Migration(this.dataSource, this.packageName);
    }

    /**
     * Create a HikariCP DataSource configured with the provided JDBC connection details.
     *
     * @param url      the JDBC connection URL (must be a non-empty string)
     * @param username the database username (must be a non-empty string)
     * @param password the database password
     * @return         a configured HikariDataSource instance
     * @throws RuntimeException if `url` or `username` is null or empty
     */
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
}

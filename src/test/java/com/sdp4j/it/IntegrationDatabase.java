package com.sdp4j.it;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Resolves, once per JVM, where the integration tests get their database. Two
 * sources, in priority order:
 *
 * <ol>
 *   <li><b>External</b> — if {@code SDP4J_IT_URL} is set, the tests run against
 *       that database (using {@code SDP4J_IT_USER} / {@code SDP4J_IT_PASSWORD}).
 *       No Docker required. <b>The tests drop and recreate the {@code public}
 *       schema before each test</b>, so this must be a throwaway database.</li>
 *   <li><b>Testcontainers</b> — otherwise, if Docker is available, a single
 *       ephemeral Postgres container is started and shared across all tests.</li>
 * </ol>
 *
 * <p>If neither is available the integration tests are skipped (not failed) via
 * a JUnit assumption.
 */
final class IntegrationDatabase {

    private final boolean available;
    private final String reason;
    private final String url;
    private final String username;
    private final String password;

    private IntegrationDatabase(boolean available, String reason,
                                String url, String username, String password) {
        this.available = available;
        this.reason = reason;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    static IntegrationDatabase resolve() {
        String externalUrl = env("SDP4J_IT_URL");
        if (externalUrl != null) {
            return new IntegrationDatabase(true, "external: " + externalUrl, externalUrl,
                    envOrDefault("SDP4J_IT_USER", "postgres"),
                    envOrDefault("SDP4J_IT_PASSWORD", ""));
        }
        if (dockerAvailable()) {
            PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:16-alpine");
            container.start(); // shared for the whole JVM; reaped by Ryuk at exit
            return new IntegrationDatabase(true, "testcontainers",
                    container.getJdbcUrl(), container.getUsername(), container.getPassword());
        }
        return new IntegrationDatabase(false,
                "no integration database: set SDP4J_IT_URL (+ SDP4J_IT_USER/SDP4J_IT_PASSWORD) "
                        + "to use a local Postgres, or start Docker for Testcontainers",
                null, null, null);
    }

    boolean isAvailable() {
        return available;
    }

    String unavailableReason() {
        return reason;
    }

    String url() {
        return url;
    }

    String username() {
        return username;
    }

    String password() {
        return password;
    }

    private static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    private static String env(String key) {
        // Accept either an environment variable (IntelliJ run config) or a
        // -Dkey=value system property (handy for `mvn test -D...`).
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        return (value == null || value.isBlank()) ? null : value;
    }

    private static String envOrDefault(String key, String fallback) {
        String value = env(key);
        return value == null ? fallback : value;
    }
}

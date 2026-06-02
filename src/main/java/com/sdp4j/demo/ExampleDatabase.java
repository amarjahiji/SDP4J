package com.sdp4j.demo;

import com.sdp4j.core.client.Sdp4jClient;
import com.sdp4j.demo.runners.MigrationDemo;

/**
 * Shared connection setup for the runnable examples in this package.
 *
 * <p>Every example builds its {@link Sdp4jClient} from here so there is a single
 * place to point them at a database. Connection settings are read from
 * environment variables, falling back to a local Postgres:
 *
 * <ul>
 *   <li>{@code SDP4J_URL}      — JDBC URL (default {@code jdbc:postgresql://localhost:5432/sdp4j_examples})</li>
 *   <li>{@code SDP4J_USER}     — username (default {@code postgres})</li>
 *   <li>{@code SDP4J_PASSWORD} — password (default {@code postgres})</li>
 * </ul>
 *
 * <p>Run {@link MigrationDemo} first to create the schema, then any other demo.
 */
public final class ExampleDatabase {

    /** Package the examples' {@code @Table} entities live in, scanned by SM4J. */
    static final String ENTITY_PACKAGE = "com.sdp4j.demo";

    private ExampleDatabase() {
    }

    public static Sdp4jClient client() {
        String url = envOrDefault("SDP4J_URL", "jdbc:postgresql://localhost:5432/sdp4j_examples");
        String username = envOrDefault("SDP4J_USER", "postgres");
        String password = envOrDefault("SDP4J_PASSWORD", "postgres");
        return new Sdp4jClient(url, username, password, ENTITY_PACKAGE);
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}

/**
 * Runnable, copy-pasteable examples of how to use SDP4J. This package ships no
 * library code — it exists purely to document the API by example, one concern
 * per file.
 *
 * <h2>Entities</h2>
 * {@link com.sdp4j.demo.models.User}, {@link com.sdp4j.demo.models.Role} and
 * {@link com.sdp4j.demo.models.Product} are {@code @Table} entities;
 * {@link com.sdp4j.demo.models.UserWithRoleDto} is a plain DTO used as a projection
 * target. Between them they exercise primary keys, {@code @NotNull}, foreign
 * keys, snake-case mapping, {@code @Length}, {@code BigDecimal}/NUMERIC and the
 * {@code @Default*} annotations.
 *
 * <h2>Examples</h2>
 * <ul>
 *   <li>{@link com.sdp4j.demo.runners.MigrationDemo} — SM4J: create and evolve the schema.</li>
 *   <li>{@link com.sdp4j.demo.runners.QueryDemo}     — SQ4J SELECT: projections, named
 *       {@code :param} WHERE clauses, joins, ordering, paging, DTO mapping.</li>
 *   <li>{@link com.sdp4j.demo.runners.InsertDemo}    — SQ4J INSERT: single and batch.</li>
 *   <li>{@link com.sdp4j.demo.runners.UpdateDemo}    — SQ4J UPDATE: entity SET + named WHERE.</li>
 *   <li>{@link com.sdp4j.demo.runners.DeleteDemo}    — SQ4J DELETE: named WHERE clauses.</li>
 *   <li>{@link com.sdp4j.demo.runners.MappingDemo}   — SQ4J: {@code @Length} and
 *       {@code BigDecimal}/NUMERIC round-trips via {@link com.sdp4j.demo.models.Product}.</li>
 *   <li>{@link com.sdp4j.demo.runners.Sps4jDemo}     — SPS4J: low-level named-parameter JDBC.</li>
 * </ul>
 *
 * <h2>Running</h2>
 * All examples connect through {@link com.sdp4j.demo.ExampleDatabase}, which
 * reads {@code SDP4J_URL} / {@code SDP4J_USER} / {@code SDP4J_PASSWORD} (with
 * local-Postgres defaults). Point those at a database, run
 * {@link com.sdp4j.demo.runners.MigrationDemo} once to create the schema, then run any
 * other example — or {@link com.sdp4j.demo.runners.RunExamples} to run them all in order.
 */
package com.sdp4j.demo;

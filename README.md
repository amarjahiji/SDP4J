# SDP4J — Simple Data Persistence for Java

[![Maven Central](https://img.shields.io/maven-central/v/io.github.amarjahiji/sdp4j.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.amarjahiji/sdp4j)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-25%2B-orange.svg)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-supported-blue.svg)](https://www.postgresql.org/)

A schema-first, lightweight persistence framework for small-to-medium Java projects.

> If you know Java and SQL and you dislike unnecessary complexity, you already know how to use it.

SDP4J sits between raw JDBC and heavyweight ORMs like Hibernate. It gives you the three things a
project actually needs — **annotation-based mapping**, **code-driven migrations**, and a
**two-layer query interface** — in a single dependency, without a persistence context, lazy-loading
proxies, dirty checking, or a query language of its own. The relational schema stays authoritative
and visible; the framework is a thin, predictable bridge across it.

PostgreSQL is the initial target dialect, chosen for its transactional DDL: a failed migration is
rolled back atomically and never leaves the schema half-changed.

> **Project status — pre-release.** SDP4J is functional and demonstrated end-to-end, but it has not
> yet been hardened with an automated test suite or published to Maven Central. The `1.0.0`
> coordinates below describe the intended first release. Until then, build from source (see
> [Building from source](#building-from-source)).

---

## Table of contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Mapping: the annotations](#mapping-the-annotations)
- [Type mapping](#type-mapping)
- [Migrations (SM4J)](#migrations-sm4j)
- [High-level queries (SQ4J)](#high-level-queries-sq4j)
- [Low-level access (SPS4J)](#low-level-access-sps4j)
- [Configuration](#configuration)
- [Design principles](#design-principles)
- [Building from source](#building-from-source)
- [Documentation](#documentation)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Acknowledgements](#acknowledgements)
- [Author](#author)
- [License](#license)

---

## Requirements

- **Java 25+**
- **PostgreSQL**
- Maven

Runtime dependencies: [HikariCP](https://github.com/brettwooldridge/HikariCP) (pooling),
[PostgreSQL JDBC driver](https://jdbc.postgresql.org/),
[ClassGraph](https://github.com/classgraph/classgraph) (classpath scanning for migrations).

## Installation

> Available once the first release is published to Maven Central. Until then, see
> [Building from source](#building-from-source).

**Maven**

```xml
<dependency>
  <groupId>io.github.amarjahiji</groupId>
  <artifactId>sdp4j</artifactId>
  <version>1.0.0</version>
</dependency>
```

**Gradle (Kotlin DSL)**

```kotlin
implementation("io.github.amarjahiji:sdp4j:1.0.0")
```

## Quick start

**1. Model a table.** One annotated class per table. Column types are inferred from the field type;
annotations describe real SQL features.

```java
@Table(name = "users", mapToSnakeCase = true)
public class User {
    @PrimaryKey
    private String id;

    @NotNull
    @Length(120)
    private String firstName;

    @NotNull
    private String lastName;

    @DefaultFalse
    private Boolean isActive;

    // getters and setters …
}
```

**2. Create a client.** Pass a JDBC URL (SDP4J configures a HikariCP pool with sensible defaults) or
your own `DataSource`. The package argument tells the migrator where to scan for `@Table` classes.

```java
Sdp4jClient client = new Sdp4jClient(
        "jdbc:postgresql://localhost:5432/app", "postgres", "secret", "com.app.model");

// or bring your own pool:
Sdp4jClient client = new Sdp4jClient("com.app.model", myDataSource);
```

**3. Migrate.** Additive changes (create table, add column, foreign keys, indexes) are derived
automatically by diffing your classes against the live database. On the first run, an empty step list
is enough to create the whole schema.

```java
SM4J sm4j = client.getSm4j();
sm4j.executeMigration(List.of()); // creates everything the classes imply
```

**4. Query.**

```java
SQ4J sq4j = client.getSq4j();

List<User> active = sq4j.select("*")
        .from(User.class)
        .where("is_active = :active AND first_name = :name")
        .set(":active", true)
        .set(":name", "Alice")
        .orderBy("last_name ASC")
        .limit(50)
        .mapTo(User.class);
```

## Mapping: the annotations

| Annotation | Target | Meaning |
|---|---|---|
| `@Table(name, mapToSnakeCase)` | class | Marks a mapped table. Name defaults to the class name (lower-cased, or snake-cased when `mapToSnakeCase = true`). |
| `@PrimaryKey` | field | Primary key column (required — every table must declare one). |
| `@NotNull` | field | `NOT NULL` constraint. |
| `@ForeignKey(mapsTo, referencedColumn, action)` | field | Foreign key. `referencedColumn` defaults to `id`; `action` is an `OnDelete` rule (`RESTRICT`, `CASCADE`, `SET_NULL`, `NO_ACTION`). |
| `@Index` | field | Creates `idx_<table>_<column>`. |
| `@UniqueKeysConstraint(keys)` | class (repeatable) | Single- or multi-column unique constraint. |
| `@Length(n)` | field (String) | `VARCHAR(n)`. Defaults to `VARCHAR(255)` when absent. |
| `@DefaultInt` / `@DefaultBigInt` / `@DefaultReal` / `@DefaultDoublePrecision` / `@DefaultString` / `@DefaultTrue` / `@DefaultFalse` | field | Typed column default, checked against the column's inferred SQL type. |

Generated object names are deterministic and readable: `fk_<table>_<column>`, `idx_<table>_<column>`,
`uq_<table>_<columns>`.

## Type mapping

| Java type | PostgreSQL type |
|---|---|
| `String` | `VARCHAR(255)` (or `VARCHAR(n)` with `@Length(n)`) |
| `Integer` / `int` | `INT` |
| `Long` / `long` | `BIGINT` |
| `Float` / `float` | `REAL` |
| `Double` / `double` | `DOUBLE PRECISION` |
| `BigDecimal` | `NUMERIC` |
| `Boolean` / `boolean` | `BOOLEAN` |
| `LocalDate` | `DATE` |
| `LocalDateTime` / `Instant` | `TIMESTAMP` |
| `UUID` | `UUID` |

## Migrations (SM4J)

SDP4J treats your annotated classes as the desired schema state.

- **Additive changes are derived.** Each run scans `@Table` classes, introspects the live database,
  and emits the `CREATE TABLE` / `ADD COLUMN` / `ADD CONSTRAINT` / `CREATE INDEX` needed to close the
  gap. This is idempotent — a class that already matches the DB produces nothing.
- **Destructive / altering changes are explicit.** Declare them as ordered steps:

```java
List<MigrationStepMetadata> steps = List.of(
        sm4j.renameColumn("users", "fname", "first_name", true), // also renames dependent objects
        sm4j.alterColumnSetType("users", "score", "numeric"),
        sm4j.dropColumn("users", "legacy_flag")
);
sm4j.executeMigration(steps);
```

Every applied step is recorded in a history table (`migrations` / `migration_steps`) so it runs
exactly once. The whole operation runs under a PostgreSQL **advisory lock** (acquired before the
schema is read) and inside a **single transaction**, so concurrent application instances are safe and
a failure rolls back atomically.

## High-level queries (SQ4J)

All WHERE/ON fragments use named `:param` placeholders bound with `.set(":param", value)`. Columns
and qualifiers are validated against your schema **before** the query runs, so typos fail fast in Java.

```java
// SELECT with join, mapping to a DTO
List<UserWithRoleDto> rows = sq4j.select("u.id", "u.first_name", "r.name")
        .from(User.class, "u")
        .innerJoin(Role.class, "r").on("u.id = r.user_id")
        .where("u.is_active = :active")
        .set(":active", true)
        .mapTo(UserWithRoleDto.class);

// IN with a collection expands automatically
sq4j.select("*").from(User.class)
        .where("first_name IN :names")
        .set(":names", List.of("Alice", "Bob", "Carol"))
        .mapTo(User.class);

// INSERT (single or batch)
sq4j.insertInto(User.class).value(user).execute();
sq4j.insertInto(User.class).values(List.of(u1, u2)).execute();

// UPDATE (null fields on the patch are skipped; PK never appears in SET)
sq4j.update(User.class)
        .set(patch)
        .where("id = :id")
        .set(":id", someId)
        .execute();

// DELETE
sq4j.deleteFrom(User.class)
        .where("is_active = :active")
        .set(":active", false)
        .execute();
```

Map to an entity (any `@Table` class) or to a plain DTO (anything else) — the framework chooses the
mapper automatically. Use `.mapToOne(Type.class)` for an `Optional`, or `.toQuery()` to inspect the
query without executing it.

## Low-level access (SPS4J)

When you want full control of the SQL and the connection, drop one level down. SPS4J wraps a single
`PreparedStatement`, swaps positional `?` for named `:param`, and translates checked exceptions away.
You own the connection's lifecycle.

```java
Connection con = null; SPS4J ps = null; ResultSet rs = null;
try {
    con = client.getDataSource().getConnection();
    ps = client.getSps4j()
            .connection(con)
            .sql("SELECT * FROM users WHERE id = :id")
            .set(":id", userId);
    rs = ps.executeQuery();
    while (rs.next()) { /* … */ }
} finally {
    client.closeResources(rs, con, ps);
}
```

A single named `set` binds every occurrence of the parameter. String literals and the PostgreSQL
`::` cast are left untouched. `ps.unwrap()` returns the raw `PreparedStatement` as an escape hatch.

## Configuration

The connection-string constructor applies conservative HikariCP defaults (min idle 5, max pool 15,
5s connection timeout, finite idle/max-lifetime, leak detection). For full control over pooling,
construct your own `DataSource` and use the `Sdp4jClient(packageName, dataSource)` constructor.

## Design principles

1. **The schema is authoritative.** One mapped class per table; DTOs for every other view.
2. **SQL is not hidden.** Builder methods mirror SQL clauses; no second query language.
3. **Immediate, predictable execution.** No persistence context, no dirty checking, no flush.
4. **Fail fast.** Columns and bindings are validated against the schema before execution.
5. **One tool.** Mapping, migrations, and querying under a single mental model.

## Building from source

```bash
git clone https://github.com/amarjahiji/SDP4J.git
cd SDP4J
mvn clean install
```

This builds the library and installs `io.github.amarjahiji:sdp4j:1.0.0` into your local Maven
repository, where other local projects can depend on it.

Maintainer release (requires a GPG key and a Maven Central Portal token in `~/.m2/settings.xml`):

```bash
mvn -Prelease clean deploy
```

## Documentation

The full project documentation — design rationale, architecture, the migration engine, and the
two-layer query interface — lives in the [project wiki](https://github.com/amarjahiji/SDP4J/wiki).

## Roadmap

- CLI for running migrations locally and in deployment pipelines
- Automated test suite (Testcontainers) and performance benchmarks
- Explicit transaction handle spanning multiple query calls
- Pluggable dialects beyond PostgreSQL
- Optional compile-time annotation processing

## Contributing

Issues and pull requests are welcome. For anything beyond a small fix, please open an issue first to
discuss the change. Bug reports are most useful with a minimal reproducing entity class and the SQL
or migration that misbehaved.

## Acknowledgements

SDP4J began as the bachelor capstone project of **Amar Jahiji** in Computer Sciences at
**South East European University (SEEU)**, Faculty of Contemporary Sciences and Technologies, under
the mentorship of **Prof. Jaumin Ajdari**.

## Author

**Amar Jahiji** — [@amarjahiji](https://github.com/amarjahiji)

## License

Released under the [MIT License](LICENSE).

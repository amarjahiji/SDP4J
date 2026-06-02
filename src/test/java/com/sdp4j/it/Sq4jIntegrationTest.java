package com.sdp4j.it;

import com.sdp4j.core.client.Sdp4jClient;
import com.sdp4j.demo.models.Product;
import com.sdp4j.demo.models.Role;
import com.sdp4j.demo.models.User;
import com.sdp4j.demo.models.UserWithRoleDto;
import com.sdp4j.sq4j.SQ4J;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SQ4J against a real Postgres: values genuinely round-trip through the database,
 * named {@code :param} WHERE clauses execute, {@code IN} collections expand,
 * joins map into DTOs, and UPDATE/DELETE report real affected-row counts.
 */
class Sq4jIntegrationTest extends PostgresIntegrationTest {

    private Sdp4jClient client;
    private SQ4J sq4j;

    @BeforeEach
    void setUp() {
        client = newClient();
        resetSchema(client.getDataSource());
        client.getSm4j().executeMigration(List.of());
        sq4j = client.getSq4j();
    }

    @Test
    void insertThenSelectRoundTrip() {
        User user = newUser("Alice", "Smith", true);
        assertEquals(1, sq4j.insertInto(User.class).value(user).execute());

        Optional<User> loaded = sq4j.select("id", "first_name", "last_name", "is_active")
                .from(User.class)
                .where("id = :id")
                .set(":id", user.getId())
                .mapToOne(User.class);

        assertTrue(loaded.isPresent());
        assertEquals("Alice", loaded.get().getFirstName());
        assertEquals("Smith", loaded.get().getLastName());
        assertTrue(loaded.get().getActive());
    }

    @Test
    void bigDecimalScaleIsPreservedThroughNumericColumn() {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setName("Desk Mat");
        product.setPrice(new BigDecimal("19.99"));
        product.setDiscontinued(false);
        sq4j.insertInto(Product.class).value(product).execute();

        Product loaded = sq4j.select("id", "name", "price")
                .from(Product.class)
                .where("id = :id")
                .set(":id", product.getId())
                .mapToOne(Product.class)
                .orElseThrow();

        assertEquals(0, loaded.getPrice().compareTo(new BigDecimal("19.99")));
        assertEquals(2, loaded.getPrice().scale());
    }

    @Test
    void inClauseExpandsAndMatchesSubset() {
        sq4j.insertInto(User.class).value(newUser("Alice", "A", true)).execute();
        sq4j.insertInto(User.class).value(newUser("Bob", "B", true)).execute();
        sq4j.insertInto(User.class).value(newUser("Carol", "C", true)).execute();

        List<User> matched = sq4j.select("id", "first_name")
                .from(User.class)
                .where("first_name IN :names")
                .set(":names", List.of("Alice", "Carol"))
                .mapTo(User.class);

        assertEquals(2, matched.size());
    }

    @Test
    void joinMapsAcrossTablesIntoDto() {
        User user = newUser("Dana", "Lee", true);
        sq4j.insertInto(User.class).value(user).execute();

        Role role = new Role();
        role.setId(UUID.randomUUID().toString());
        role.setName("admin");
        role.setUserId(user.getId());
        sq4j.insertInto(Role.class).value(role).execute();

        List<UserWithRoleDto> rows = sq4j.select("u.id", "u.first_name", "u.last_name", "u.is_active", "r.name")
                .from(User.class, "u")
                .innerJoin(Role.class, "r").on("u.id = r.user_id")
                .where("r.name = :role")
                .set(":role", "admin")
                .mapTo(UserWithRoleDto.class);

        assertEquals(1, rows.size());
        assertEquals("Dana", rows.getFirst().getFirstName());
        assertEquals("admin", rows.getFirst().getName());
    }

    @Test
    void updateChangesRowAndReportsAffectedCount() {
        User user = newUser("Eve", "Stone", true);
        sq4j.insertInto(User.class).value(user).execute();

        User patch = new User();
        patch.setFirstName("Evelyn");
        int affected = sq4j.update(User.class)
                .set(patch)
                .where("id = :id")
                .set(":id", user.getId())
                .execute();

        assertEquals(1, affected);
        String name = scalarString(client.getDataSource(),
                "SELECT first_name FROM users WHERE id = '" + user.getId() + "'");
        assertEquals("Evelyn", name);
    }

    @Test
    void deleteRemovesRowAndReportsAffectedCount() {
        User user = newUser("Frank", "Moore", false);
        sq4j.insertInto(User.class).value(user).execute();

        int affected = sq4j.deleteFrom(User.class)
                .where("id = :id")
                .set(":id", user.getId())
                .execute();

        assertEquals(1, affected);
        assertFalse(sq4j.select("id").from(User.class)
                .where("id = :id").set(":id", user.getId())
                .mapToOne(User.class).isPresent());
    }

    private User newUser(String firstName, String lastName, boolean active) {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(active);
        return user;
    }
}

package com.sdp4j.demo;

import com.sdp4j.core.client.Sdp4jClient;
import com.sdp4j.sq4j.SQ4J;
import com.sdp4j.sq4j.query.InsertQuery;
import com.sdp4j.sq4j.render.PostgresDialect;
import com.sdp4j.sq4j.render.RenderContext;

import java.util.UUID;

public class InsertDemo {

    private final SQ4J sq4j;

    public InsertDemo() {
        this.sq4j = new Sdp4jClient("", "", "", "com.sdp4j.demo").getSq4j();
    }

    static void main(String[] args) {
        InsertDemo demo = new InsertDemo();

        demo.case1_insertFullEntity();
        demo.case2_insertPartialEntitySkipsNulls();
        demo.case3_insertOnlyPk();
        demo.case4_inspectInsertAst();
        demo.case5_previewRenderedSql();
        demo.case6_rejectNullEntity();
        demo.case7_rejectAllNullEntity();
        demo.case8_insertRoleAcrossFk();
        demo.case9_batchInsertSameSignature();
        demo.case10_batchInsertSignatureMismatchRejected();
        demo.case11_batchInsertEmptyListRejected();
        demo.case12_batchInsertNullElementRejected();
    }

    private void case1_insertFullEntity() {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFirstName("Alice");
        user.setLastName("Smith");
        user.setActive(true);

        int affected = sq4j.insertInto(User.class).value(user).execute();
        System.out.println("case1 inserted=" + affected);
    }

    private void case2_insertPartialEntitySkipsNulls() {
        // lastName and isActive are null — they're omitted from the INSERT so
        // the DB defaults (e.g. is_active default-false) take effect.
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFirstName("Bob");

        int affected = sq4j.insertInto(User.class).value(user).execute();
        System.out.println("case2 inserted=" + affected);
    }

    private void case3_insertOnlyPk() {
        User user = new User();
        user.setId(UUID.randomUUID().toString());

        int affected = sq4j.insertInto(User.class).value(user).execute();
        System.out.println("case3 inserted=" + affected);
    }

    private void case4_inspectInsertAst() {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFirstName("Carol");

        InsertQuery query = sq4j.insertInto(User.class).value(user).toQuery();
        System.out.println("case4 table=" + query.target().name()
                + " columns=" + query.columns()
                + " values=" + query.values());
    }

    private void case5_previewRenderedSql() {
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setFirstName("Dave");
        user.setLastName("Johnson");
        user.setActive(false);

        InsertQuery query = sq4j.insertInto(User.class).value(user).toQuery();
        RenderContext ctx = new RenderContext(new PostgresDialect());
        query.render(ctx);
        System.out.println("case5 sql=" + ctx.sql());
        System.out.println("case5 bindings=" + ctx.bindings());
    }

    private void case6_rejectNullEntity() {
        try {
            sq4j.insertInto(User.class).value(null).execute();
        } catch (RuntimeException e) {
            System.out.println("case6 rejected: " + e.getMessage());
        }
    }

    private void case7_rejectAllNullEntity() {
        try {
            sq4j.insertInto(User.class).value(new User()).execute();
        } catch (RuntimeException e) {
            System.out.println("case7 rejected: " + e.getMessage());
        }
    }

    private void case9_batchInsertSameSignature() {
        User u1 = new User();
        u1.setId(java.util.UUID.randomUUID().toString());
        u1.setFirstName("Alice");
        u1.setLastName("Smith");

        User u2 = new User();
        u2.setId(java.util.UUID.randomUUID().toString());
        u2.setFirstName("Bob");
        u2.setLastName("Jones");

        int affected = sq4j.insertInto(User.class)
                .values(java.util.List.of(u1, u2))
                .execute();
        System.out.println("case9 inserted=" + affected);
    }

    private void case10_batchInsertSignatureMismatchRejected() {
        User u1 = new User();
        u1.setId(java.util.UUID.randomUUID().toString());
        u1.setFirstName("Carol");
        u1.setLastName("Stone");
        u1.setActive(true);

        User u2 = new User();
        u2.setId(java.util.UUID.randomUUID().toString());
        u2.setFirstName("Dan");
        // last_name and is_active are null on u2 — signature differs

        try {
            sq4j.insertInto(User.class).values(java.util.List.of(u1, u2)).execute();
        } catch (RuntimeException e) {
            System.out.println("case10 rejected: " + e.getMessage());
        }
    }

    private void case11_batchInsertEmptyListRejected() {
        try {
            sq4j.insertInto(User.class).values(java.util.List.of()).execute();
        } catch (RuntimeException e) {
            System.out.println("case11 rejected: " + e.getMessage());
        }
    }

    private void case12_batchInsertNullElementRejected() {
        User u = new User();
        u.setId(java.util.UUID.randomUUID().toString());
        try {
            sq4j.insertInto(User.class)
                    .values(java.util.Arrays.asList(u, null))
                    .execute();
        } catch (RuntimeException e) {
            System.out.println("case12 rejected: " + e.getMessage());
        }
    }

    private void case8_insertRoleAcrossFk() {
        // Demonstrates that INSERT works on any @Table-annotated class,
        // even with a foreign-key column populated by the caller.
        Role role = new Role();
        role.setId(UUID.randomUUID().toString());
        role.setName("admin");
        role.setUserId(UUID.randomUUID().toString());

        int affected = sq4j.insertInto(Role.class).value(role).execute();
        System.out.println("case8 inserted=" + affected);
    }
}

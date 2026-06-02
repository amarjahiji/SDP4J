package com.sdp4j.demo.runners;

import com.sdp4j.demo.ExampleDatabase;
import com.sdp4j.demo.models.Role;
import com.sdp4j.demo.models.User;
import com.sdp4j.sq4j.SQ4J;
import com.sdp4j.sq4j.queryinternals.InsertQuery;
import com.sdp4j.sq4j.renderers.RenderContext;

import java.util.UUID;

public class InsertDemo {

    private final SQ4J sq4j;

    public InsertDemo() {
        this.sq4j = ExampleDatabase.client().getSq4j();
    }

    public static void main(String[] args) {
        InsertDemo demo = new InsertDemo();

        demo.case1_insertFullEntity();
        demo.case2_partialEntityNullsBecomeSqlNull();
        demo.case3_insertOnlyPk();
        demo.case4_inspectInsertAst();
        demo.case5_previewRenderedSql();
        demo.case6_rejectNullEntity();
        demo.case7_insertRoleAcrossFk();
        demo.case8_batchInsert();
        demo.case9_batchInsertEmptyListRejected();
        demo.case10_batchInsertNullElementRejected();
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

    private void case2_partialEntityNullsBecomeSqlNull() {
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
        RenderContext ctx = new RenderContext();
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

    private void case7_insertRoleAcrossFk() {
        Role role = new Role();
        role.setId(UUID.randomUUID().toString());
        role.setName("admin");
        role.setUserId(UUID.randomUUID().toString());

        int affected = sq4j.insertInto(Role.class).value(role).execute();
        System.out.println("case7 inserted=" + affected);
    }

    private void case8_batchInsert() {
        User u1 = new User();
        u1.setId(UUID.randomUUID().toString());
        u1.setFirstName("Alice");
        u1.setLastName("Smith");
        u1.setActive(true);

        User u2 = new User();
        u2.setId(UUID.randomUUID().toString());
        u2.setFirstName("Bob");

        int affected = sq4j.insertInto(User.class)
                .values(java.util.List.of(u1, u2))
                .execute();
        System.out.println("case8 inserted=" + affected);
    }

    private void case9_batchInsertEmptyListRejected() {
        try {
            sq4j.insertInto(User.class).values(java.util.List.of()).execute();
        } catch (RuntimeException e) {
            System.out.println("case9 rejected: " + e.getMessage());
        }
    }

    private void case10_batchInsertNullElementRejected() {
        User u = new User();
        u.setId(UUID.randomUUID().toString());
        try {
            sq4j.insertInto(User.class)
                    .values(java.util.Arrays.asList(u, null))
                    .execute();
        } catch (RuntimeException e) {
            System.out.println("case10 rejected: " + e.getMessage());
        }
    }
}

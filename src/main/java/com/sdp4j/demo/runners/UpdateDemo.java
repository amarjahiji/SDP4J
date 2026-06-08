package com.sdp4j.demo.runners;

import com.sdp4j.demo.ExampleDatabase;
import com.sdp4j.demo.models.User;
import com.sdp4j.sq4j.SQ4J;
import com.sdp4j.sq4j.queryinternals.UpdateQuery;
import com.sdp4j.sq4j.renderers.RenderContext;

import java.util.UUID;

public class UpdateDemo {

    private final SQ4J sq4j;

    public UpdateDemo() {
        this.sq4j = ExampleDatabase.client().getSq4j();
    }

    public static void main(String[] args) {
        UpdateDemo demo = new UpdateDemo();
        demo.case1_updateFullEntityWherePk();
        demo.case2_updatePartialEntitySkipsNulls();
        demo.case3_updateWithAlias();
        demo.case4_updateAllRowsWhenNoWhere();
        demo.case5_pkOnSuppliedEntityIsIgnoredFromSet();
        demo.case6_rejectNullEntity();
        demo.case7_rejectEntityWithNoNonPkFields();
        demo.case8_unknownColumnInWhereRejected();
        demo.case9_inspectUpdateAst();
        demo.case10_previewRenderedSql();
    }

    private void case1_updateFullEntityWherePk() {
        User newValues = new User();
        newValues.setFirstName("Alice");
        newValues.setSurname("Smith");
        newValues.setActive(true);

        int affected = sq4j.update(User.class)
                .set(newValues)
                .where("id = :id")
                .set(":id", "aa743699-a783-4363-bd75-bcb2ab2734a1")
                .execute();
        System.out.println("case1 updated=" + affected);
    }

    private void case2_updatePartialEntitySkipsNulls() {
        User patch = new User();
        patch.setFirstName("Alicia");

        int affected = sq4j.update(User.class)
                .set(patch)
                .where("id = :id")
                .set(":id", "aa743699-a783-4363-bd75-bcb2ab2734a1")
                .execute();
        System.out.println("case2 updated=" + affected);
    }

    private void case3_updateWithAlias() {
        User patch = new User();
        patch.setActive(false);

        int affected = sq4j.update(User.class, "u")
                .set(patch)
                .where("u.first_name = :first_name")
                .set(":first_name", "Alice")
                .execute();
        System.out.println("case3 updated=" + affected);
    }

    private void case4_updateAllRowsWhenNoWhere() {
        User patch = new User();
        patch.setActive(true);

        int affected = sq4j.update(User.class).set(patch).execute();
        System.out.println("case4 updated=" + affected);
    }

    private void case5_pkOnSuppliedEntityIsIgnoredFromSet() {
        User patch = new User();
        patch.setId(UUID.randomUUID().toString());
        patch.setFirstName("Bob");

        UpdateQuery query = sq4j.update(User.class)
                .set(patch)
                .where("id = :id")
                .set(":id", "some-existing-id")
                .toQuery();
        System.out.println("case5 columns=" + query.columnsToSet());
    }

    private void case6_rejectNullEntity() {
        try {
            sq4j.update(User.class).set(null).execute();
        } catch (RuntimeException e) {
            System.out.println("case6 rejected: " + e.getMessage());
        }
    }

    private void case7_rejectEntityWithNoNonPkFields() {
        try {
            sq4j.update(User.class).set(new User()).execute();
        } catch (RuntimeException e) {
            System.out.println("case7 rejected: " + e.getMessage());
        }
    }

    private void case8_unknownColumnInWhereRejected() {
        User patch = new User();
        patch.setFirstName("Carol");
        try {
            sq4j.update(User.class)
                    .set(patch)
                    .where("ghost = :ghost")
                    .set(":ghost", 1)
                    .execute();
        } catch (RuntimeException e) {
            System.out.println("case8 rejected: " + e.getMessage());
        }
    }

    private void case9_inspectUpdateAst() {
        User patch = new User();
        patch.setFirstName("Dave");
        patch.setSurname("Man");
        patch.setActive(true);

        UpdateQuery query = sq4j.update(User.class)
                .set(patch)
                .where("id = :id")
                .set(":id", "abc")
                .toQuery();
        System.out.println("case9 table=" + query.target().name()
                + " columns=" + query.columnsToSet()
                + " setValues=" + query.setValues()
                + " where=" + query.whereSql()
                + " whereBindings=" + query.whereBindings());
    }

    private void case10_previewRenderedSql() {
        User patch = new User();
        patch.setFirstName("Eve");
        patch.setSurname("Williams");
        patch.setActive(false);

        UpdateQuery query = sq4j.update(User.class, "u")
                .set(patch)
                .where("u.is_active = :is_active AND u.first_name IN :names")
                .set(":is_active", true)
                .set(":names", java.util.List.of("Alice", "Bob"))
                .toQuery();
        RenderContext ctx = new RenderContext();
        query.render(ctx);
        System.out.println("case10 sql=" + ctx.sql());
        System.out.println("case10 bindings=" + ctx.bindings());
    }
}

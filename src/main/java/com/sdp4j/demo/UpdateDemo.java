package com.sdp4j.demo;

import com.sdp4j.core.client.Sdp4jClient;
import com.sdp4j.sq4j.SQ4J;
import com.sdp4j.sq4j.query.UpdateQuery;
import com.sdp4j.sq4j.render.PostgresDialect;
import com.sdp4j.sq4j.render.RenderContext;

import java.util.UUID;

public class UpdateDemo {

    private final SQ4J sq4j;

    public UpdateDemo() {
        this.sq4j = new Sdp4jClient("", "", "", "com.sdp4j.demo").getSq4j();
    }

    static void main(String[] args) {
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
        demo.case11_batchPatchesByPk();
        demo.case12_batchPatchesMissingPkRejected();
        demo.case13_batchPatchesSignatureMismatchRejected();
        demo.case14_batchPatchesEmptyListRejected();
    }

    private void case1_updateFullEntityWherePk() {
        User newValues = new User();
        newValues.setFirstName("Alice");
        newValues.setLastName("Smith");
        newValues.setActive(true);

        int affected = sq4j.update(User.class)
                .set(newValues)
                .where("id = ?", "some-existing-id")
                .execute();
        System.out.println("case1 updated=" + affected);
    }

    private void case2_updatePartialEntitySkipsNulls() {
        // Only first_name is provided; last_name and is_active stay untouched
        // because they're null on the supplied entity.
        User patch = new User();
        patch.setFirstName("Alicia");

        int affected = sq4j.update(User.class)
                .set(patch)
                .where("id = ?", "some-existing-id")
                .execute();
        System.out.println("case2 updated=" + affected);
    }

    private void case3_updateWithAlias() {
        User patch = new User();
        patch.setActive(false);

        int affected = sq4j.update(User.class, "u")
                .set(patch)
                .where("u.first_name = ?", "Alice")
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
        // Even though `id` is set on the patch, it never appears in SET —
        // the builder filters PK columns out so you don't accidentally
        // overwrite the row's identity.
        User patch = new User();
        patch.setId(UUID.randomUUID().toString());
        patch.setFirstName("Bob");

        UpdateQuery query = sq4j.update(User.class)
                .set(patch)
                .where("id = ?", "some-existing-id")
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
        // Empty entity — nothing to update.
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
                    .where("ghost = ?", 1)
                    .execute();
        } catch (RuntimeException e) {
            System.out.println("case8 rejected: " + e.getMessage());
        }
    }

    private void case9_inspectUpdateAst() {
        User patch = new User();
        patch.setFirstName("Dave");
        patch.setActive(true);

        UpdateQuery query = sq4j.update(User.class)
                .set(patch)
                .where("id = ?", "abc")
                .toQuery();
        System.out.println("case9 table=" + query.target().name()
                + " columns=" + query.columnsToSet()
                + " setValues=" + query.setValues()
                + " where=" + query.whereSql()
                + " whereBindings=" + query.whereBindings());
    }

    private void case11_batchPatchesByPk() {
        User p1 = new User();
        p1.setId("user-1");
        p1.setFirstName("Alice");
        p1.setActive(true);

        User p2 = new User();
        p2.setId("user-2");
        p2.setFirstName("Bob");
        p2.setActive(true);

        int affected = sq4j.update(User.class)
                .patches(java.util.List.of(p1, p2))
                .execute();
        System.out.println("case11 updated=" + affected);
    }

    private void case12_batchPatchesMissingPkRejected() {
        User p1 = new User();
        p1.setId("user-1");
        p1.setFirstName("Alice");

        User p2 = new User();
        // no id on p2
        p2.setFirstName("Bob");

        try {
            sq4j.update(User.class).patches(java.util.List.of(p1, p2)).execute();
        } catch (RuntimeException e) {
            System.out.println("case12 rejected: " + e.getMessage());
        }
    }

    private void case13_batchPatchesSignatureMismatchRejected() {
        User p1 = new User();
        p1.setId("user-1");
        p1.setFirstName("Alice");
        p1.setActive(true);

        User p2 = new User();
        p2.setId("user-2");
        p2.setFirstName("Bob");
        // is_active null on p2 — signature differs from p1

        try {
            sq4j.update(User.class).patches(java.util.List.of(p1, p2)).execute();
        } catch (RuntimeException e) {
            System.out.println("case13 rejected: " + e.getMessage());
        }
    }

    private void case14_batchPatchesEmptyListRejected() {
        try {
            sq4j.update(User.class).patches(java.util.List.of()).execute();
        } catch (RuntimeException e) {
            System.out.println("case14 rejected: " + e.getMessage());
        }
    }

    private void case10_previewRenderedSql() {
        User patch = new User();
        patch.setFirstName("Eve");
        patch.setLastName("Williams");
        patch.setActive(false);

        UpdateQuery query = sq4j.update(User.class, "u")
                .set(patch)
                .where("u.is_active = ? AND u.first_name IN ?", true,
                        java.util.List.of("Alice", "Bob"))
                .toQuery();
        RenderContext ctx = new RenderContext(new PostgresDialect());
        query.render(ctx);
        System.out.println("case10 sql=" + ctx.sql());
        System.out.println("case10 bindings=" + ctx.bindings());
    }
}

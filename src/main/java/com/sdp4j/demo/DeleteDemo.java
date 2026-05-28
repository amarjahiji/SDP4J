package com.sdp4j.demo;

import com.sdp4j.core.client.Sdp4jClient;
import com.sdp4j.sq4j.SQ4J;
import com.sdp4j.sq4j.query.DeleteQuery;
import com.sdp4j.sq4j.render.PostgresDialect;
import com.sdp4j.sq4j.render.RenderContext;

import java.util.List;

public class DeleteDemo {

    private final SQ4J sq4j;

    public DeleteDemo() {
        this.sq4j = new Sdp4jClient("", "", "", "com.sdp4j.demo").getSq4j();
    }

    static void main(String[] args) {
        DeleteDemo demo = new DeleteDemo();

        demo.case1_deleteAllRowsFromTable();
        demo.case2_deleteWithSimpleWhere();
        demo.case3_deleteWithBindings();
        demo.case4_deleteWithCollectionExpansion();
        demo.case5_deleteWithAlias();
        demo.case6_unknownColumnInWhereRejected();
        demo.case7_unknownColumnInProjectionStyleRejected();
        demo.case8_inspectDeleteQueryAst();
        demo.case9_previewRenderedSql();
        demo.case10_complexBooleanWhere();
    }

    private void case1_deleteAllRowsFromTable() {
        int affected = sq4j.deleteFrom(User.class).execute();
        System.out.println("case1 deleted=" + affected);
    }

    private void case2_deleteWithSimpleWhere() {
        int affected = sq4j.deleteFrom(User.class)
                .where("is_active = ?", false)
                .execute();
        System.out.println("case2 deleted=" + affected);
    }

    private void case3_deleteWithBindings() {
        int affected = sq4j.deleteFrom(User.class)
                .where("first_name = ? AND last_name = ?", "Alice", "Smith")
                .execute();
        System.out.println("case3 deleted=" + affected);
    }

    private void case4_deleteWithCollectionExpansion() {
        int affected = sq4j.deleteFrom(User.class)
                .where("first_name IN ?", List.of("Alice", "Bob", "Carol"))
                .execute();
        System.out.println("case4 deleted=" + affected);
    }

    private void case5_deleteWithAlias() {
        int affected = sq4j.deleteFrom(User.class, "u")
                .where("u.is_active = ?", false)
                .execute();
        System.out.println("case5 deleted=" + affected);
    }

    private void case6_unknownColumnInWhereRejected() {
        try {
            sq4j.deleteFrom(User.class)
                    .where("ghost = ?", 1)
                    .execute();
        } catch (RuntimeException e) {
            System.out.println("case6 rejected: " + e.getMessage());
        }
    }

    private void case7_unknownColumnInProjectionStyleRejected() {
        // Sanity check: validator still catches typos when alias is used.
        try {
            sq4j.deleteFrom(User.class, "u")
                    .where("u.ghost = ?", 1)
                    .execute();
        } catch (RuntimeException e) {
            System.out.println("case7 rejected: " + e.getMessage());
        }
    }

    private void case8_inspectDeleteQueryAst() {
        DeleteQuery query = sq4j.deleteFrom(User.class)
                .where("is_active = ?", false)
                .toQuery();
        System.out.println("case8 table=" + query.from().name()
                + " where=" + query.whereSql()
                + " bindings=" + query.whereBindings());
    }

    private void case9_previewRenderedSql() {
        DeleteQuery query = sq4j.deleteFrom(User.class, "u")
                .where("u.first_name IN ? AND u.is_active = ?", List.of("Alice", "Bob"), false)
                .toQuery();
        RenderContext ctx = new RenderContext(new PostgresDialect());
        query.render(ctx);
        System.out.println("case9 sql=" + ctx.sql());
        System.out.println("case9 bindings=" + ctx.bindings());
    }

    private void case10_complexBooleanWhere() {
        int affected = sq4j.deleteFrom(User.class)
                .where("is_active = ? AND (first_name = ? OR last_name = ?)", false, "Alice", "Smith")
                .execute();
        System.out.println("case10 deleted=" + affected);
    }
}

package com.sdp4j.demo.runners;

import com.sdp4j.demo.ExampleDatabase;
import com.sdp4j.demo.models.Role;
import com.sdp4j.demo.models.User;
import com.sdp4j.demo.models.UserWithRoleDto;
import com.sdp4j.sq4j.SQ4J;
import com.sdp4j.sq4j.queryinternals.SelectQuery;
import com.sdp4j.sq4j.renderers.RenderContext;

import java.util.List;
import java.util.Optional;

public class QueryDemo {

    private final SQ4J sq4j;

    public QueryDemo() {
        this.sq4j = ExampleDatabase.client().getSq4j();
    }

    public static void main(String[] args) {
        QueryDemo demo = new QueryDemo();

//        demo.case1_selectAllUserColumns();
//        demo.case2_selectSubsetOfColumns();
//        demo.case3_singleColumn();
//        demo.case4_simpleEqualityWithBinding();
//        demo.case5_andClauseWithMultipleBindings();
//        demo.case6_orClause();
//        demo.case7_mixedAndOrWithGroups();
//        demo.case8_likePattern();
//        demo.case9_inWithCollectionExpansion();
//        demo.case10_isNullAndIsNotNull();
//        demo.case11_notExpression();
//        demo.case12_qualifiedColumnInWhere();
//        demo.case13_orderByAsc();
//        demo.case14_orderByDesc();
//        demo.case15_orderByMultiple();
//        demo.case16_limit();
//        demo.case17_limitWithOffset();
//        demo.case18_mapToOne();
//        demo.case19_mapToOneEmpty();
//        demo.case20_toQueryInspection();
//        demo.case21_renderForSqlPreview();
//        demo.case22_unknownColumnInSelectIsRejected();
//        demo.case23_unknownColumnInWhereIsRejected();
//        demo.case24_unknownTableQualifierIsRejected();
//        demo.case25_mismatchedBindingsRejected();
//        demo.case26_inWithMixedScalarAndCollection();
//        demo.case27_selectStar();
//        demo.case28_selectStarWithWhere();
//        demo.case29_distinct();
//        demo.case30_distinctWithWhereAndOrder();
//        demo.case31_innerJoinWithAliases();
//        demo.case32_leftJoin();
//        demo.case33_rightJoin();
//        demo.case34_fullOuterJoin();
//        demo.case35_joinWithoutAliasUsesTableNameAsQualifier();
//        demo.case36_ambiguousBareColumnInWhereIsRejected();
//        demo.case37_qualifyingResolvesAmbiguity();
//        demo.case38_joinPlusDistinctPlusWhereAndOrder();
//        demo.case39_multipleJoins();
//        demo.case40_unknownQualifierInOnIsRejected();
//        demo.case41_mapToDtoSingleTable();
        demo.case42_mapToDtoAcrossJoin();
//        demo.case43_dtoFieldsWithNoMatchingColumnStayNull();
//        demo.case44_resultColumnsWithNoMatchingDtoFieldIgnored();
    }

    private void case1_selectAllUserColumns() {
        List<User> users = sq4j.select("id", "first_name", "last_name", "is_active")
                .from(User.class)
                .mapTo(User.class);
        System.out.println("case1 rows=" + users.size());
    }

    private void case2_selectSubsetOfColumns() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .mapTo(User.class);
        System.out.println("case2 rows=" + users.size());
    }

    private void case3_singleColumn() {
        List<User> users = sq4j.select("id").from(User.class).mapTo(User.class);
        System.out.println("case3 rows=" + users.size());
    }

    private void case4_simpleEqualityWithBinding() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .where("is_active = :is_active")
                .set(":is_active", true)
                .mapTo(User.class);
        System.out.println("case4 rows=" + users.size());
    }

    private void case5_andClauseWithMultipleBindings() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .where("is_active = :is_active AND first_name = :first_name")
                .set(":is_active", true)
                .set(":first_name", "Alice")
                .mapTo(User.class);
        System.out.println("case5 rows=" + users.size());
    }

    private void case6_orClause() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .where("first_name = :first OR first_name = :second")
                .set(":first", "Alice")
                .set(":second", "Bob")
                .mapTo(User.class);
        System.out.println("case6 rows=" + users.size());
    }

    private void case7_mixedAndOrWithGroups() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .where("is_active = :is_active AND (first_name = :first OR first_name = :second)")
                .set(":is_active", true)
                .set(":first", "Alice")
                .set(":second", "Bob")
                .mapTo(User.class);
        System.out.println("case7 rows=" + users.size());
    }

    private void case8_likePattern() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .where("first_name LIKE :pattern")
                .set(":pattern", "Al%")
                .mapTo(User.class);
        System.out.println("case8 rows=" + users.size());
    }

    private void case9_inWithCollectionExpansion() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .where("first_name IN :names")
                .set(":names", List.of("Alice", "Bob", "Carol"))
                .mapTo(User.class);
        System.out.println("case9 rows=" + users.size());
    }

    private void case10_isNullAndIsNotNull() {
        List<User> users = sq4j.select("id", "first_name", "last_name")
                .from(User.class)
                .where("first_name IS NOT NULL AND last_name IS NULL")
                .mapTo(User.class);
        System.out.println("case10 rows=" + users.size());
    }

    private void case11_notExpression() {
        List<User> users = sq4j.select("id", "is_active")
                .from(User.class)
                .where("NOT (is_active = :is_active)")
                .set(":is_active", false)
                .mapTo(User.class);
        System.out.println("case11 rows=" + users.size());
    }

    private void case12_qualifiedColumnInWhere() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .where("users.first_name = :first_name")
                .set(":first_name", "Alice")
                .mapTo(User.class);
        System.out.println("case12 rows=" + users.size());
    }

    private void case13_orderByAsc() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .orderBy("first_name ASC")
                .mapTo(User.class);
        System.out.println("case13 rows=" + users.size());
    }

    private void case14_orderByDesc() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .orderBy("first_name DESC")
                .mapTo(User.class);
        System.out.println("case14 rows=" + users.size());
    }

    private void case15_orderByMultiple() {
        List<User> users = sq4j.select("id", "first_name", "last_name")
                .from(User.class)
                .orderBy("last_name ASC, first_name ASC")
                .mapTo(User.class);
        System.out.println("case15 rows=" + users.size());
    }

    private void case16_limit() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .limit(5)
                .mapTo(User.class);
        System.out.println("case16 rows=" + users.size());
    }

    private void case17_limitWithOffset() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .orderBy("id ASC")
                .limit(5, 10)
                .mapTo(User.class);
        System.out.println("case17 rows=" + users.size());
    }

    private void case18_mapToOne() {
        Optional<User> maybe = sq4j.select("id", "first_name")
                .from(User.class)
                .where("first_name = :first_name")
                .set(":first_name", "Alice")
                .limit(1)
                .mapToOne(User.class);
        System.out.println("case18 present=" + maybe.isPresent());
    }

    private void case19_mapToOneEmpty() {
        Optional<User> none = sq4j.select("id", "first_name")
                .from(User.class)
                .where("first_name = :first_name")
                .set(":first_name", "____no_such_user____")
                .mapToOne(User.class);
        System.out.println("case19 empty=" + none.isEmpty());
    }

    private void case20_toQueryInspection() {
        SelectQuery query = sq4j.select("id", "first_name")
                .from(User.class)
                .where("is_active = :is_active AND first_name > :first_name")
                .set(":is_active", true)
                .set(":first_name", "A")
                .orderBy("first_name DESC")
                .limit(10)
                .toQuery();
        System.out.println("case20 fields=" + query.fields().size()
                + " where=" + query.whereSql()
                + " orderBy=" + query.orderBySql()
                + " limit=" + query.limit());
    }

    private void case21_renderForSqlPreview() {
        SelectQuery query = sq4j.select("id", "first_name")
                .from(User.class)
                .where("first_name = :first_name OR first_name IN :names")
                .set(":first_name", "Alice")
                .set(":names", List.of("Bob", "Carol"))
                .orderBy("first_name ASC")
                .limit(5, 10)
                .toQuery();
        RenderContext ctx = new RenderContext();
        query.render(ctx);
        System.out.println("case21 sql=" + ctx.sql());
        System.out.println("case21 bindings=" + ctx.bindings());
    }

    private void case22_unknownColumnInSelectIsRejected() {
        try {
            sq4j.select("ghost_column").from(User.class).mapTo(User.class);
        } catch (RuntimeException e) {
            System.out.println("case22 rejected: " + e.getMessage());
        }
    }

    private void case23_unknownColumnInWhereIsRejected() {
        try {
            sq4j.select("id").from(User.class)
                    .where("ghost = :ghost")
                    .set(":ghost", 1)
                    .mapTo(User.class);
        } catch (RuntimeException e) {
            System.out.println("case23 rejected: " + e.getMessage());
        }
    }

    private void case24_unknownTableQualifierIsRejected() {
        try {
            sq4j.select("id").from(User.class)
                    .where("roles.id = :id")
                    .set(":id", 1)
                    .mapTo(User.class);
        } catch (RuntimeException e) {
            System.out.println("case24 rejected: " + e.getMessage());
        }
    }

    private void case25_mismatchedBindingsRejected() {
        try {
            sq4j.select("id").from(User.class)
                    .where("first_name = :first_name AND last_name = :last_name")
                    .set(":first_name", "Alice")
                    .mapTo(User.class);
        } catch (RuntimeException e) {
            System.out.println("case25 rejected: " + e.getMessage());
        }
    }

    private void case26_inWithMixedScalarAndCollection() {
        List<User> users = sq4j.select("id", "first_name")
                .from(User.class)
                .where("is_active = :is_active AND first_name IN :names")
                .set(":is_active", true)
                .set(":names", List.of("Alice", "Bob"))
                .mapTo(User.class);
        System.out.println("case26 rows=" + users.size());
    }

    private void case27_selectStar() {
        List<User> users = sq4j.select("*").from(User.class).mapTo(User.class);
        System.out.println("case27 rows=" + users.size());
    }

    private void case28_selectStarWithWhere() {
        List<User> users = sq4j.select("*")
                .from(User.class)
                .where("is_active = :is_active")
                .set(":is_active", true)
                .mapTo(User.class);
        System.out.println("case28 rows=" + users.size());
    }

    private void case29_distinct() {
        List<User> users = sq4j.select("first_name")
                .from(User.class)
                .distinct()
                .mapTo(User.class);
        System.out.println("case29 rows=" + users.size());
    }

    private void case30_distinctWithWhereAndOrder() {
        List<User> users = sq4j.select("first_name", "last_name")
                .from(User.class)
                .distinct()
                .where("is_active = :is_active")
                .set(":is_active", true)
                .orderBy("first_name ASC")
                .limit(10)
                .mapTo(User.class);
        System.out.println("case30 rows=" + users.size());
    }

    private void case31_innerJoinWithAliases() {
        List<User> users = sq4j.select("u.id", "u.first_name", "r.name")
                .from(User.class, "u")
                .innerJoin(Role.class, "r").on("u.id = r.user_id")
                .mapTo(User.class);
        System.out.println("case31 rows=" + users.size());
    }

    private void case32_leftJoin() {
        List<User> users = sq4j.select("u.id", "r.name")
                .from(User.class, "u")
                .leftJoin(Role.class, "r").on("u.id = r.user_id")
                .mapTo(User.class);
        System.out.println("case32 rows=" + users.size());
    }

    private void case33_rightJoin() {
        List<User> users = sq4j.select("u.id", "r.name")
                .from(User.class, "u")
                .rightJoin(Role.class, "r").on("u.id = r.user_id")
                .mapTo(User.class);
        System.out.println("case33 rows=" + users.size());
    }

    private void case34_fullOuterJoin() {
        List<User> users = sq4j.select("u.id", "r.name")
                .from(User.class, "u")
                .fullOuterJoin(Role.class, "r").on("u.id = r.user_id")
                .mapTo(User.class);
        System.out.println("case34 rows=" + users.size());
    }

    private void case35_joinWithoutAliasUsesTableNameAsQualifier() {
        List<User> users = sq4j.select("users.id", "roles.name")
                .from(User.class)
                .innerJoin(Role.class, null).on("users.id = roles.user_id")
                .mapTo(User.class);
        System.out.println("case35 rows=" + users.size());
    }

    private void case36_ambiguousBareColumnInWhereIsRejected() {
        try {
            sq4j.select("u.id")
                    .from(User.class, "u")
                    .innerJoin(Role.class, "r").on("u.id = r.user_id")
                    .where("id = :id")
                    .set(":id", "abc")
                    .mapTo(User.class);
        } catch (RuntimeException e) {
            System.out.println("case36 rejected: " + e.getMessage());
        }
    }

    private void case37_qualifyingResolvesAmbiguity() {
        List<User> users = sq4j.select("u.id")
                .from(User.class, "u")
                .innerJoin(Role.class, "r").on("u.id = r.user_id")
                .where("u.id = :id")
                .set(":id", "abc")
                .mapTo(User.class);
        System.out.println("case37 rows=" + users.size());
    }

    private void case38_joinPlusDistinctPlusWhereAndOrder() {
        List<User> users = sq4j.select("u.first_name")
                .from(User.class, "u")
                .innerJoin(Role.class, "r").on("u.id = r.user_id")
                .distinct()
                .where("u.is_active = :is_active AND r.name IN :names")
                .set(":is_active", true)
                .set(":names", List.of("admin", "editor"))
                .orderBy("u.first_name ASC")
                .limit(50)
                .mapTo(User.class);
        System.out.println("case38 rows=" + users.size());
    }

    private void case39_multipleJoins() {
        List<User> users = sq4j.select("u.id", "r.name")
                .from(User.class, "u")
                .innerJoin(Role.class, "r").on("u.id = r.user_id")
                .leftJoin(Role.class, "r2").on("u.id = r2.user_id")
                .where("r.name = :name")
                .set(":name", "admin")
                .mapTo(User.class);
        System.out.println("case39 rows=" + users.size());
    }

    private void case40_unknownQualifierInOnIsRejected() {
        try {
            sq4j.select("u.id")
                    .from(User.class, "u")
                    .innerJoin(Role.class, "r").on("u.id = ghost.user_id")
                    .mapTo(User.class);
        } catch (RuntimeException e) {
            System.out.println("case40 rejected: " + e.getMessage());
        }
    }

    private void case41_mapToDtoSingleTable() {
        List<UserWithRoleDto> rows = sq4j.select("id", "first_name", "last_name", "is_active")
                .from(User.class)
                .mapTo(UserWithRoleDto.class);
        System.out.println("case41 rows=" + rows.size());
    }

    private void case42_mapToDtoAcrossJoin() {
        List<UserWithRoleDto> rows = sq4j.select("u.id", "u.first_name", "u.surname", "u.is_active", "r.name")
                .from(User.class, "u")
                .innerJoin(Role.class, "r").on("u.id = r.user_id")
                .mapTo(UserWithRoleDto.class);
        System.out.println("case42 rows=" + rows.size());
    }

    private void case43_dtoFieldsWithNoMatchingColumnStayNull() {
        List<UserWithRoleDto> rows = sq4j.select("id", "first_name")
                .from(User.class)
                .mapTo(UserWithRoleDto.class);
        System.out.println("case43 rows=" + rows.size());
    }

    private void case44_resultColumnsWithNoMatchingDtoFieldIgnored() {
        List<UserWithRoleDto> rows = sq4j.select("u.id", "u.first_name", "r.name", "r.user_id")
                .from(User.class, "u")
                .innerJoin(Role.class, "r").on("u.id = r.user_id")
                .mapTo(UserWithRoleDto.class);
        System.out.println("case44 rows=" + rows.size());
    }
}

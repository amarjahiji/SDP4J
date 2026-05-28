package com.sdp4j.sq4j.dsl;

public interface SelectJoinStep extends SelectWhereStep {

    SelectOnStep innerJoin(Class<?> entityClass, String alias);

    SelectOnStep leftJoin(Class<?> entityClass, String alias);

    SelectOnStep rightJoin(Class<?> entityClass, String alias);

    SelectOnStep fullOuterJoin(Class<?> entityClass, String alias);

    SelectJoinStep distinct();
}

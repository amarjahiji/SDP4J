package com.sdp4j.sq4j.dsl;

public interface SelectFromStep {

    SelectJoinStep from(Class<?> entityClass);

    SelectJoinStep from(Class<?> entityClass, String alias);
}

package com.sdp4j.sq4j.steps;

public interface SelectFromStep {

    SelectJoinStep from(Class<?> entityClass);

    SelectJoinStep from(Class<?> entityClass, String alias);
}

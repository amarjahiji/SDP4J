package com.sdp4j.sq4j.dsl;

public interface SelectWhereStep extends SelectOrderByStep {

    SelectOrderByStep where(String sql, Object... values);
}
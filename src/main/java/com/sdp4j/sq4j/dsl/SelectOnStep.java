package com.sdp4j.sq4j.dsl;

public interface SelectOnStep {

    SelectJoinStep on(String sql, Object... values);
}

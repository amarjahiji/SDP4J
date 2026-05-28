package com.sdp4j.sq4j.dsl;

public interface SelectOrderByStep extends SelectLimitStep {

    SelectLimitStep orderBy(String sql);
}
package com.sdp4j.sq4j.steps;

public interface SelectOrderByStep extends SelectLimitStep {

    SelectLimitStep orderBy(String sql);
}
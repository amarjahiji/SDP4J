package com.sdp4j.sq4j.steps;

public interface SelectWhereStep extends SelectOrderByStep {

    SelectBindStep where(String sql);
}

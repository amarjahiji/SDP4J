package com.sdp4j.sq4j.steps;

public interface SelectBindStep extends SelectOrderByStep {

    SelectBindStep set(String name, Object value);
}

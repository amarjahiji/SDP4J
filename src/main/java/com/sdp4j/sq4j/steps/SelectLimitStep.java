package com.sdp4j.sq4j.steps;

public interface SelectLimitStep extends SelectFetchStep {

    SelectFetchStep limit(int limit);

    SelectFetchStep limit(int limit, int offset);
}
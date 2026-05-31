package com.sdp4j.sq4j.steps;

public interface UpdateWhereStep extends UpdateExecuteStep {

    UpdateBindStep where(String sql);
}

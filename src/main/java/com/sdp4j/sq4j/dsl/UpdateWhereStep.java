package com.sdp4j.sq4j.dsl;

public interface UpdateWhereStep extends UpdateExecuteStep {

    UpdateExecuteStep where(String sql, Object... values);
}

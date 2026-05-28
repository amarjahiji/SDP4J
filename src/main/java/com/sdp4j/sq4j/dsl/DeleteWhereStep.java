package com.sdp4j.sq4j.dsl;

public interface DeleteWhereStep extends DeleteExecuteStep {

    DeleteExecuteStep where(String sql, Object... values);
}

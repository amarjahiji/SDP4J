package com.sdp4j.sq4j.steps;

public interface DeleteWhereStep extends DeleteExecuteStep {

    DeleteBindStep where(String sql);
}

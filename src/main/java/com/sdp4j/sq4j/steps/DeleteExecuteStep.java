package com.sdp4j.sq4j.steps;

import com.sdp4j.sq4j.queryinternals.DeleteQuery;

public interface DeleteExecuteStep {

    int execute();

    DeleteQuery toQuery();
}

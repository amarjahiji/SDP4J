package com.sdp4j.sq4j.steps;

import com.sdp4j.sq4j.queryinternals.UpdateQuery;

public interface UpdateExecuteStep {

    int execute();

    UpdateQuery toQuery();
}

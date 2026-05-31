package com.sdp4j.sq4j.steps;

import com.sdp4j.sq4j.queryinternals.InsertQuery;

public interface InsertExecuteStep {

    int execute();

    InsertQuery toQuery();
}

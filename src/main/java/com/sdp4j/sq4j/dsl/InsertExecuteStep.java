package com.sdp4j.sq4j.dsl;

import com.sdp4j.sq4j.query.InsertQuery;

public interface InsertExecuteStep {

    int execute();

    InsertQuery toQuery();
}

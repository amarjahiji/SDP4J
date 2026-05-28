package com.sdp4j.sq4j.dsl;

import com.sdp4j.sq4j.query.UpdateQuery;

public interface UpdateExecuteStep {

    int execute();

    UpdateQuery toQuery();
}

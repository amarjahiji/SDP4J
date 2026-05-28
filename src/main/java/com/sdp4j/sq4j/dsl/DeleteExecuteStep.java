package com.sdp4j.sq4j.dsl;

import com.sdp4j.sq4j.query.DeleteQuery;

public interface DeleteExecuteStep {

    int execute();

    DeleteQuery toQuery();
}

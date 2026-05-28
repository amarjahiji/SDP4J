package com.sdp4j.sq4j.dsl;

import java.util.List;

public interface UpdateSetStep<T> {

    UpdateWhereStep set(T entity);

    UpdateExecuteStep patches(List<T> patches);
}

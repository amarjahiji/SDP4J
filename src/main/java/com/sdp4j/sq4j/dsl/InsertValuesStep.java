package com.sdp4j.sq4j.dsl;

import java.util.List;

public interface InsertValuesStep<T> {

    InsertExecuteStep value(T entity);

    InsertExecuteStep values(List<T> entities);
}

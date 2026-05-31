package com.sdp4j.sq4j.steps;

public interface UpdateSetStep<T> {

    UpdateWhereStep set(T entity);
}

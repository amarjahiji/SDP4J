package com.sdp4j.sq4j.dsl;

import com.sdp4j.sq4j.query.SelectQuery;

import java.util.List;
import java.util.Optional;

public interface SelectFetchStep {

    <T> List<T> mapTo(Class<T> type);

    <T> Optional<T> mapToOne(Class<T> type);

    SelectQuery toQuery();
}
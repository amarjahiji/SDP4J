package com.sdp4j.sq4j.validation;

import java.util.List;

public record PreparedWhereClause(String renderedSql, List<Object> orderedBindings) {

}

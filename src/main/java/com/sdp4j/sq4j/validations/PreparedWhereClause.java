package com.sdp4j.sq4j.validations;

import java.util.List;
import java.util.Set;

public record PreparedWhereClause(String renderedSql, List<Object> orderedBindings, Set<String> usedNames) {

}

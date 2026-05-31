package com.sdp4j.sq4j.queryinternals;

import com.sdp4j.sq4j.enums.JoinType;

import java.util.Collections;
import java.util.List;

public record JoinSpec(JoinType type, TableRef joinedTable, String onSql, List<Object> onBindings) {

    public JoinSpec(JoinType type, TableRef joinedTable, String onSql, List<Object> onBindings) {
        this.type = type;
        this.joinedTable = joinedTable;
        this.onSql = onSql;
        this.onBindings = onBindings == null ? List.of() : Collections.unmodifiableList(onBindings);
    }
}

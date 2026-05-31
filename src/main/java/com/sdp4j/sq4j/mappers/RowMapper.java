package com.sdp4j.sq4j.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface RowMapper<T> {

    T map(ResultSet rs) throws SQLException;
}
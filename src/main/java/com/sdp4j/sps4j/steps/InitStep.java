package com.sdp4j.sps4j.steps;

import com.sdp4j.core.exception.Sps4jException;
import com.sdp4j.sps4j.SPS4J;

import java.sql.Connection;

public final class InitStep implements ConnectionStep, SqlStep {

    private Connection connection;

    @Override
    public SqlStep connection(Connection connection) {
        if (connection == null) {
            throw new Sps4jException("Cannot use a null connection");
        }
        this.connection = connection;
        return this;
    }

    @Override
    public SPS4J sql(String namedSql) {
        return new SPS4J(connection, namedSql);
    }
}

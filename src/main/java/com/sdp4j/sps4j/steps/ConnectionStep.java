package com.sdp4j.sps4j.steps;

import java.sql.Connection;

public interface ConnectionStep {

    SqlStep connection(Connection connection);
}

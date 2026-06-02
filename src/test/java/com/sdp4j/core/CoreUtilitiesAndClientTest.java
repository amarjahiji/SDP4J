package com.sdp4j.core;

import com.sdp4j.core.client.Sdp4jClient;
import com.sdp4j.core.exception.Sdp4jConfigurationException;
import com.sdp4j.core.exception.Sdp4jValidationException;
import com.sdp4j.core.util.CommonUtil;
import com.sdp4j.sm4j.SM4J;
import com.sdp4j.sq4j.SQ4J;
import com.sdp4j.testsupport.JdbcProxySupport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreUtilitiesAndClientTest {

    @Test
    void commonUtilHandlesStringsCollectionsSnakeCaseAndSqlIdentifiers() {
        assertEquals("user_profile", CommonUtil.toSnakeCase("UserProfile"));
        assertEquals("already_snake", CommonUtil.toSnakeCase("already_snake"));
        assertEquals("", CommonUtil.toSnakeCase(""));
        assertTrue(CommonUtil.isValidCollection(List.of("x")));
        assertFalse(CommonUtil.isValidCollection(List.of()));
        assertTrue(CommonUtil.isValidString(" value "));
        assertFalse(CommonUtil.isValidString(" "));
        assertTrue(CommonUtil.areValidSqlIdentifiers("users", "_internal_1"));
        assertFalse(CommonUtil.areValidSqlIdentifiers("1bad"));
        assertFalse(CommonUtil.areValidSqlIdentifiers("bad-name"));
        assertEquals("configured", CommonUtil.getOrDefault("configured", "fallback"));
        assertEquals("fallback", CommonUtil.getOrDefault(" ", "fallback"));
    }

    @Test
    void clientWiresProvidedDataSourceAndSubApis() {
        JdbcProxySupport.RecordingDataSource dataSource = JdbcProxySupport.recordingDataSource();

        Sdp4jClient client = new Sdp4jClient("com.sdp4j.core", dataSource);

        assertSame(dataSource, client.getDataSource());
        assertEquals("com.sdp4j.core", client.getPackageName());
        assertInstanceOf(SM4J.class, client.getSm4j());
        assertInstanceOf(SQ4J.class, client.getSq4j());
        assertDoesNotThrow(client::getSps4j);
    }

    @Test
    void clientValidatesJdbcUrlConstructorArguments() {
        assertThrows(Sdp4jConfigurationException.class,
                () -> new Sdp4jClient(" ", "user", "secret", "com.example"));
        assertThrows(Sdp4jConfigurationException.class,
                () -> new Sdp4jClient("jdbc:postgresql://localhost/db", " ", "secret", "com.example"));
        assertThrows(Sdp4jValidationException.class,
                () -> new Sdp4jClient(" ", JdbcProxySupport.recordingDataSource()));
    }

    @Test
    void closeResourcesClosesAllResourcesAndSuppressesCloseFailures() throws Exception {
        Sdp4jClient client = new Sdp4jClient("com.sdp4j.core", JdbcProxySupport.recordingDataSource());
        boolean[] closed = new boolean[2];
        ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{ResultSet.class},
                (_, method, _) -> {
                    if ("close".equals(method.getName())) {
                        closed[0] = true;
                        throw new RuntimeException("ignored");
                    }
                    return null;
                });
        Connection connection = (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                (_, method, _) -> {
                    if ("close".equals(method.getName())) {
                        closed[1] = true;
                        throw new RuntimeException("ignored");
                    }
                    return null;
                });

        assertDoesNotThrow(() -> client.closeResources(resultSet, connection, (com.sdp4j.sps4j.SPS4J[]) null));
        assertTrue(closed[0]);
        assertTrue(closed[1]);
    }
}

package com.sdp4j.core.exception;

public final class Sdp4jJdbcException extends Sdp4jException {

    public Sdp4jJdbcException(String message) {
        super(message);
    }

    public Sdp4jJdbcException(String message, Throwable cause) {
        super(message, cause);
    }
}
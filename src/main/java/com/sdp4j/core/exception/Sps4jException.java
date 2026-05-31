package com.sdp4j.core.exception;

public class Sps4jException extends RuntimeException {

    public Sps4jException(String message) {
        super(message);
    }

    public Sps4jException(String message, Throwable cause) {
        super(message, cause);
    }
}

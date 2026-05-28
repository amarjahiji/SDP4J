package com.sdp4j.core.exception;

public class Sq4jException extends RuntimeException {

    public Sq4jException(String message) {
        super(message);
    }

    public Sq4jException(String message, Throwable cause) {
        super(message, cause);
    }
}
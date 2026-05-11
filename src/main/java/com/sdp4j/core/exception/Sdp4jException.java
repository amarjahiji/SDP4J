package com.sdp4j.core.exception;

public class Sdp4jException extends RuntimeException {

    public Sdp4jException(String message) {
        super(message);
    }

    public Sdp4jException(String message, Throwable cause) {
        super(message, cause);
    }
}
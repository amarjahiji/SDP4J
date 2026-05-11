package com.sdp4j.core.exception;

public final class Sdp4jValidationException extends Sdp4jException {

    public Sdp4jValidationException(String message) {
        super(message);
    }

    public Sdp4jValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
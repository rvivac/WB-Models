package com.wbscouting.api.exception;

public class InvalidApplicationException extends RuntimeException {

    public InvalidApplicationException(String message) {
        super(message);
    }

    public InvalidApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

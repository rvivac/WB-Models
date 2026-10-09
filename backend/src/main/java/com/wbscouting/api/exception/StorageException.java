package com.wbscouting.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StorageException extends RuntimeException {
    private final HttpStatus status;
    private final String title;
    private final String errorCode;

    // Métodos getters explícitos para blindagem completa do ambiente
    public HttpStatus getStatus() {
        return this.status;
    }

    public String getTitle() {
        return this.title;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    public StorageException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.title = "Storage Error";
        this.errorCode = "STORAGE_ERROR";
    }

    public StorageException(String message, HttpStatus status, String title, String errorCode) {
        super(message);
        this.status = status;
        this.title = title;
        this.errorCode = errorCode;
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.title = "Storage Error";
        this.errorCode = "STORAGE_ERROR";
    }

    public StorageException(String message, Throwable cause, HttpStatus status, String title, String errorCode) {
        super(message, cause);
        this.status = status;
        this.title = title;
        this.errorCode = errorCode;
    }
}

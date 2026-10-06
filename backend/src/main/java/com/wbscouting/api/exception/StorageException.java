package com.wbscouting.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StorageException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String title;

    // 🔥 GETTERS EXPLICITOS (Lombok @Getter nao gera no MavenWrapper 3.6.3 Render)
    // GlobalExceptionHandler linhas 115-122 chama ex.getStatus/ex.getTitle/ex.getErrorCode - obrigatorio existencia explicita.
    public HttpStatus getStatus() { return this.status; }
    public String getErrorCode() { return this.errorCode; }
    public String getTitle() { return this.title; }

    public StorageException(String message) {
        this(message, HttpStatus.BAD_GATEWAY, "STORAGE_ERROR", "Erro de Armazenamento");
    }

    public StorageException(String message, Throwable cause) {
        this(message, cause, HttpStatus.BAD_GATEWAY, "STORAGE_ERROR", "Erro de Armazenamento");
    }

    public StorageException(String message, HttpStatus status, String errorCode, String title) {
        super(message);
        this.status = status != null ? status : HttpStatus.BAD_GATEWAY;
        this.errorCode = errorCode != null ? errorCode : "STORAGE_ERROR";
        this.title = title != null ? title : "Erro de Armazenamento";
    }

    public StorageException(String message, Throwable cause, HttpStatus status, String errorCode, String title) {
        super(message, cause);
        this.status = status != null ? status : HttpStatus.BAD_GATEWAY;
        this.errorCode = errorCode != null ? errorCode : "STORAGE_ERROR";
        this.title = title != null ? title : "Erro de Armazenamento";
    }
}

package com.wbscouting.api.exception;

public class InvalidSortPropertyException extends RuntimeException {

    private final String property;

    public InvalidSortPropertyException(String property) {
        super(String.format("Propriedade de ordenação inválida ou não permitida: '%s'.", property));
        this.property = property;
    }

    public String getProperty() {
        return property;
    }
}

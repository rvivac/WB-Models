package com.wbscouting.api.exception;

public class DuplicatePromotionException extends RuntimeException {
    public DuplicatePromotionException(String message) {
        super(message);
    }
}

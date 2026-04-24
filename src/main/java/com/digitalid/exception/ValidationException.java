package com.digitalid.exception;

public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
    public ValidationException() {
        super("A required field is missing, blank, or conflicts with the current identity state.");
    }
}
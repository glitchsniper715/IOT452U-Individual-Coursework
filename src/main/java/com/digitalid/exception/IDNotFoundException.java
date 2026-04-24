package com.digitalid.exception;

/**
 * Thrown when a Digital ID lookup is attempted for an idNumber that does not
 * exist in the repository.
 */
public class IDNotFoundException extends RuntimeException {

    /**
     * Creates a new IDNotFoundException with a message that should include
     * the idNumber that was not found, e.g. "Digital ID not found: DIG-001".
     */
    public IDNotFoundException(String message) {
        super(message);
    }
}
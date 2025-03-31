package com.oc.api.passport.exception;

public class InvalidInputException extends RuntimeException {

    /**
     * BsDDJsonValidationException with message.
     * @param message
     */
    public InvalidInputException(String message) {
        super(message);
    }
}

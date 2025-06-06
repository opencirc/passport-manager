package com.opencirc.api.passport.exception;

public class InvalidInputException extends RuntimeException {

    /**
     * JsonValidationException with message.
     * @param message
     */
    public InvalidInputException(String message) {
        super(message);
    }
}

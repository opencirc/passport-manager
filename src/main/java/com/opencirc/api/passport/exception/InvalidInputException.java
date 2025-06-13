package com.opencirc.api.passport.exception;

public class InvalidInputException extends RuntimeException {

    /**
     * JsonValidationException with message.
     * @param message
     */
    public InvalidInputException(String message) {
        super(message);
    }

    /**
     * JsonValidationException with message and cause.
     * @param message
     * @param cause
     */
    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}

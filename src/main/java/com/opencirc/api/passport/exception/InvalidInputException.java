package com.opencirc.api.passport.exception;

public class InvalidInputException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * InvalidInputException with message.
     * @param message
     */
    public InvalidInputException(String message) {
        super(message);
    }

    /**
     * InvalidInputException with message and cause.
     * @param message
     * @param cause
     */
    public InvalidInputException(String message, Throwable cause) {
        super(message, cause);
    }
}

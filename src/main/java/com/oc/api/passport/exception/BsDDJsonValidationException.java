package com.oc.api.passport.exception;

public class BsDDJsonValidationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * BsDDJsonValidationException with message.
     * @param message
     */
    public BsDDJsonValidationException(String message) {
        super(message);
    }

    /**
     * BsDDJsonValidationException with message and cause.
     * @param message
     * @param cause
     */
    public BsDDJsonValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}

package com.oc.api.passport.exception;

public class JsonValidationException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * JsonValidationException with message.
     * @param message
     */
    public JsonValidationException(String message) {
        super(message);
    }

    /**
     * JsonValidationException with message and cause.
     * @param message
     * @param cause
     */
    public JsonValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}

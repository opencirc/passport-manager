package com.oc.api.passport.adapter.bsdd.exception;

import com.oc.api.passport.exception.JsonValidationException;

public class BsDDJsonValidationException extends JsonValidationException {

    /**
     *
     */
    private static final long serialVersionUID = 2L;

    /**
     * JsonValidationException with message.
     * @param message
     */
    public BsDDJsonValidationException(String message) {
        super(message);
    }

    /**
     * JsonValidationException with message and cause.
     * @param message
     * @param cause
     */
    public BsDDJsonValidationException(String message, Throwable cause) {
        super(message, cause);
    }

}

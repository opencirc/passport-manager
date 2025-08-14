package com.opencirc.api.passport.exception;

public class CacheSerializationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * CacheSerializationException with message.
     * @param message
     */
    public CacheSerializationException(String message) {
        super(message);
    }

    /**
     * CacheSerializationException with message and cause.
     * @param message
     * @param cause
     */
    public CacheSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}

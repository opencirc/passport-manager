package com.opencirc.api.passport.exception;

/** CacheSerializationException. */
public class CacheSerializationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /** CacheSerializationException with message and cause. */
  public CacheSerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}

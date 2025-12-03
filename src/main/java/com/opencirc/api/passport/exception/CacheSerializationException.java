package com.opencirc.api.passport.exception;

/** CacheSerializationException. */
public class CacheSerializationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /** CacheSerializationException with a message. */
  public CacheSerializationException(String message) {
    super(message);
  }

  /** CacheSerializationException with cause. */
  public CacheSerializationException(Throwable cause) {
    super(cause);
  }

  /** CacheSerializationException with message and cause. */
  public CacheSerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}

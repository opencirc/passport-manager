package com.opencirc.api.passport.exception;

/** Thrown when authentication fails. */
public class AuthenticationException extends RuntimeException {

  /** Serialization version UID for AuthenticationException class. */
  private static final long serialVersionUID = 1L;

  /**
   * AuthenticationException with a message.
   */
  public AuthenticationException(String message) {
    super(message);
  }

  /**
   * AuthenticationException with message and cause.
   */
  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}

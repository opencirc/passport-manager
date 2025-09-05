package com.opencirc.api.passport.exception;

/** Thrown when authentication fails. */
public class AuthenticationException extends RuntimeException {

  /** Serialization version UID for AuthenticationException class. */
  private static final long serialVersionUID = 1L;

  /**
   * AuthenticationException with message.
   *
   * @param message
   */
  public AuthenticationException(String message) {
    super(message);
  }

  /**
   * AuthenticationException with message and cause.
   *
   * @param message
   * @param cause
   */
  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}

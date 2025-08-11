package com.opencirc.api.passport.exception;

import java.io.Serial;

public class AuthenticationException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

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

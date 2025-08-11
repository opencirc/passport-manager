package com.opencirc.api.passport.exception;

import java.io.Serial;

public class JsonValidationException extends Exception {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * JsonValidationException with message.
   *
   * @param message
   */
  public JsonValidationException(String message) {
    super(message);
  }

  /**
   * JsonValidationException with message and cause.
   *
   * @param message
   * @param cause
   */
  public JsonValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}

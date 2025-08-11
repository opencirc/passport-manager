package com.opencirc.api.passport.exception;

import java.io.Serial;

public class InvalidInputException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * InvalidInputException with message.
   *
   * @param message
   */
  public InvalidInputException(String message) {
    super(message);
  }

  /**
   * InvalidInputException with message and cause.
   *
   * @param message
   * @param cause
   */
  public InvalidInputException(String message, Throwable cause) {
    super(message, cause);
  }
}

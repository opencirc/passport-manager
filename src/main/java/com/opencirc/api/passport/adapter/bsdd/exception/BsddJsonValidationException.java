package com.opencirc.api.passport.adapter.bsdd.exception;

import com.opencirc.api.passport.exception.JsonValidationException;

public class BsddJsonValidationException extends JsonValidationException {

  /** Serialization version UID for BsddJsonValidationException class. */
  private static final long serialVersionUID = 2L;

  /**
   * JsonValidationException with message.
   *
   * @param message
   */
  public BsddJsonValidationException(String message) {
    super(message);
  }

  /**
   * JsonValidationException with message and cause.
   *
   * @param message
   * @param cause
   */
  public BsddJsonValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}

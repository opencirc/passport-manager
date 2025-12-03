package com.opencirc.api.passport.adapter.bsdd.exception;

import com.opencirc.api.passport.exception.JsonValidationException;

/**
 * BsDD JSON validation exception.
 */
public class BsddJsonValidationException extends JsonValidationException {
  private static final long serialVersionUID = 1L;

  /**
   * JsonValidationException with a message.
   */
  public BsddJsonValidationException(String message) {
    super(message);
  }

  /**
   * JsonValidationException with message and cause.
   */
  public BsddJsonValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}

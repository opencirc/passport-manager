package com.opencirc.api.passport.exception;

/** Exception for invalid JSON. */
public class JsonValidationException extends Exception {
  private static final long serialVersionUID = 1L;

  /** JsonValidationException with a message. */
  public JsonValidationException(String message) {
    super(message);
  }

  /** JsonValidationException with message and cause. */
  public JsonValidationException(String message, Throwable cause) {
    super(message, cause);
  }
}

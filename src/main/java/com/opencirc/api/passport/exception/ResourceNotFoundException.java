package com.opencirc.api.passport.exception;

public class ResourceNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * ResourceNotFoundException with message.
   *
   * @param message
   */
  public ResourceNotFoundException(String message) {
    super(message);
  }

  /**
   * ResourceNotFoundException with message and cause.
   *
   * @param message
   * @param cause
   */
  public ResourceNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}

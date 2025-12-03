package com.opencirc.api.passport.exception;

/** ResourceNotFoundException. */
public class ResourceNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * ResourceNotFoundException with a message.
   */
  public ResourceNotFoundException(String message) {
    super(message);
  }
}

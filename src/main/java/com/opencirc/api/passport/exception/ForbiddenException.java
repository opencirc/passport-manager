package com.opencirc.api.passport.exception;

/** Raised when an authenticated actor is not allowed to act on the targeted resource. */
public class ForbiddenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with a message. */
  public ForbiddenException(String message) {
    super(message);
  }
}

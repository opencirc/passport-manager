package com.opencirc.api.passport.exception;

/** Exception for invalid inputs. */
public class InvalidInputException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** InvalidInputException with a message. */
  public InvalidInputException(String message) {
    super(message);
  }
}

package com.opencirc.api.passport.dto;

/** DTO for wrapping a message in API responses. */
public class StatusResponseDto {

  /** The message of the response. */
  private String message;

  /** Instantiates StatusResponseDto. */
  public StatusResponseDto(String message) {
    this.message = message;
  }

  /** Returns the message. */
  public String getMessage() {
    return message;
  }

  /** Sets the message. */
  public void setMessage(String message) {
    this.message = message;
  }
}

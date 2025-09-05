package com.opencirc.api.passport.dto;

/** DTO for wrapping a message in API responses. */
public class StatusResponseDto {

  /** The message of the response. */
  private String message;

  /**
   * Instantiates StatusResponseDto.
   *
   * @param message
   */
  public StatusResponseDto(String message) {
    this.message = message;
  }

  /**
   * Returns the message.
   *
   * @return the message string
   */
  public String getMessage() {
    return message;
  }

  /**
   * Sets the message.
   *
   * @param message the new message string
   */
  public void setMessage(String message) {
    this.message = message;
  }
}

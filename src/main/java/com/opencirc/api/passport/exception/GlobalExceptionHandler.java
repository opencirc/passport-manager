package com.opencirc.api.passport.exception;

import com.opencirc.api.passport.constants.AppConstants;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handler for JWT exception.
   *
   * @param ex - authentication exception
   * @return response
   */
  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Object> handleAuthenticationException(AuthenticationException ex) {
    if (ex.getMessage().contains(AppConstants.ERR_INVALID_TOKEN)) {
      ErrorResponse errorResponse = new ErrorResponse("Token Expired ", ex.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    } else if (ex.getMessage().contains(AppConstants.ERR_INVALID_CREDENTIALS)) {
      ErrorResponse errorResponse = new ErrorResponse("Invalid Credentials ", ex.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    } else {
      ErrorResponse errorResponse = new ErrorResponse("Authentication Error", ex.getMessage());
      return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }
  }

  /**
   * Handler for Invalid input exception.
   *
   * @param ex - invalid input exception
   * @param request
   * @return response
   */
  @ExceptionHandler(InvalidInputException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ResponseEntity<?> handleInvalidInputException(
      InvalidInputException ex, WebRequest request) {
    ErrorResponse errorResponse = new ErrorResponse("Invalid Input", ex.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  /**
   * Handler for Resource not found exception.
   *
   * @param ex - resource not found exception
   * @return response
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Object> handleResourceNotFound(ResourceNotFoundException ex) {
    ErrorResponse errorResponse = new ErrorResponse("Not Found", ex.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }

  /**
   * Handler for All other exception.
   *
   * @param ex - exception
   * @return response
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleGenericException(Exception ex) {
    ErrorResponse errorResponse = new ErrorResponse("Internal Server Error", ex.getMessage());
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public static class ErrorResponse {

    /** the error type. */
    private String error;

    /** Error message. */
    private String message;

    /**
     * ErrorResponse with message.
     *
     * @param errorType
     * @param errMessage
     */
    public ErrorResponse(String errorType, String errMessage) {
      this.error = errorType;
      this.message = errMessage;
    }

    /**
     * The Error type Getter.
     *
     * @return error
     */
    public String getError() {
      return error;
    }

    /**
     * The Error type Setter.
     *
     * @param passedError
     */
    public void setError(String passedError) {
      this.error = passedError;
    }

    /**
     * Message Getter.
     *
     * @return message
     */
    public String getMessage() {
      return message;
    }

    /**
     * Message Setter.
     *
     * @param errorMessage
     */
    public void setMessage(String errorMessage) {
      this.message = errorMessage;
    }
  }
}

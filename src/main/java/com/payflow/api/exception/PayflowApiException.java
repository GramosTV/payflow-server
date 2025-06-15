package com.payflow.api.exception;

import org.springframework.http.HttpStatus;

/** Generic exception for PayFlow API errors with customizable HTTP status code */
public class PayflowApiException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final HttpStatus status;

  public PayflowApiException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }

  public PayflowApiException(String message, Throwable cause, HttpStatus status) {
    super(message, cause);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }
}

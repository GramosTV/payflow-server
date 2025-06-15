package com.payflow.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception thrown when a bad request is made to the API. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public BadRequestException(final String message) {
    super(message);
  }

  public BadRequestException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

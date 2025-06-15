package com.payflow.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception thrown when a conflict occurs during API operations. */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ConflictException(final String message) {
    super(message);
  }

  public ConflictException(final String message, final Throwable cause) {
    super(message, cause);
  }
}

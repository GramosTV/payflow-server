package com.payflow.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exception thrown when access to a resource is forbidden. */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ForbiddenException(final String message) {
    super(message);
  }
}

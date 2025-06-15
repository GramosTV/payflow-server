package com.payflow.api.exception;

import java.util.Map;

/** Standard error response for API errors */
public class ErrorResponse {
  private int status;
  private String message;
  private String timestamp;

  public ErrorResponse(int status, String message, String timestamp) {
    this.status = status;
    this.message = message;
    this.timestamp = timestamp;
  }

  public int getStatus() {
    return status;
  }

  public void setStatus(int status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }
}

/** Extended error response for validation errors with field-specific error messages */
class ValidationErrorResponse extends ErrorResponse {
  private Map<String, String> errors;

  public ValidationErrorResponse(
      int status, String message, String timestamp, Map<String, String> errors) {
    super(status, message, timestamp);
    this.errors = errors;
  }

  public Map<String, String> getErrors() {
    return errors;
  }

  public void setErrors(Map<String, String> errors) {
    this.errors = errors;
  }
}

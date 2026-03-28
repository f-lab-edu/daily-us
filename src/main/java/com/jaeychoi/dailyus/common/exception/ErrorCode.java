package com.jaeychoi.dailyus.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USR_001", "Email is already in use."),
  NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USR_002", "Nickname is already in use."),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_001", "Email or password is invalid."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_002", "Authentication is required."),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_003", "Token is invalid or expired."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_004",
      "You do not have permission to access this resource."),
  INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_005",
      "Refresh token is invalid or expired.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

}

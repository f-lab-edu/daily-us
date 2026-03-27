package com.jaeychoi.dailyus.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "USR_001", "Email is already in use."),
  NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USR_002", "Nickname is already in use.");

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

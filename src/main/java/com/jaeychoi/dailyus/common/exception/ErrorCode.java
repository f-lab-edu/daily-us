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
      "Refresh token is invalid or expired."),
  GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "GRP_001", "Group not found."),
  GROUP_ALREADY_JOINED(HttpStatus.CONFLICT, "GRP_002", "Group is already in use."),
  GROUP_MEMBER_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "GRP_003", "Group is full."),
  GROUP_USER_JOIN_LIMIT(HttpStatus.CONFLICT, "GRP_004",
      "User has reached the maximum number of groups they can join."),
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USR_003", "User not found."),
  SELF_FOLLOW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "USR_004", "You cannot follow yourself."),
  FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "USR_005", "Follow relationship already exists."),
  FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "USR_006", "Follow relationship not found."),
  POST_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "PST_001", "At least one image is required."),
  POST_HASHTAG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "PST_003",
      "No more than 10 hashtags are allowed.");

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

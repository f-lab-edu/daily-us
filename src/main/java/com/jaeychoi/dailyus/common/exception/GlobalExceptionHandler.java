package com.jaeychoi.dailyus.common.exception;

import com.jaeychoi.dailyus.common.web.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
    loggingError(e);
    HttpStatus status = e.getErrorCode().getStatus();
    String errorCode = e.getErrorCode().getCode();
    String errorMessage = e.getMessage();
    return ResponseEntity.status(status).body(ApiResponse.error(errorCode, errorMessage));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleInternalException(Exception e) {
    loggingError(e);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("INTERNAL", "An unexpected error occurred"));
  }

  private void loggingError(Exception e) {
    log.error("Unhandled exception: {}", e.getMessage(), e);
  }
}

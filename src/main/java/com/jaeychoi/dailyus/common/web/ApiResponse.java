package com.jaeychoi.dailyus.common.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;

@JsonInclude(Include.NON_NULL)
public record ApiResponse<T>(String code, String message, T data, LocalDateTime timeStamp) {

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>("OK", null, data, LocalDateTime.now());
  }

  public static <T> ApiResponse<T> error(String code, String message) {
    return new ApiResponse<>(code, message, null, LocalDateTime.now());
  }

}

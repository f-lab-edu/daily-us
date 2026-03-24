package com.jaeychoi.dailyus.auth.controller;

import com.jaeychoi.dailyus.auth.dto.SignUpRequest;
import com.jaeychoi.dailyus.auth.dto.SignUpResponse;
import com.jaeychoi.dailyus.auth.service.SignUpService;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final SignUpService signUpService;

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
    SignUpResponse response = signUpService.signUp(request);
    return ApiResponse.success(response);
  }
}

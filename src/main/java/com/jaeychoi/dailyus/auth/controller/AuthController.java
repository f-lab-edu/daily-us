package com.jaeychoi.dailyus.auth.controller;

import com.jaeychoi.dailyus.auth.dto.SignInRequest;
import com.jaeychoi.dailyus.auth.dto.SignUpRequest;
import com.jaeychoi.dailyus.auth.dto.SignUpResponse;
import com.jaeychoi.dailyus.auth.dto.TokenResponse;
import com.jaeychoi.dailyus.auth.service.SignInService;
import com.jaeychoi.dailyus.auth.service.SignUpService;
import com.jaeychoi.dailyus.common.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
  private final SignInService signInService;

  @PostMapping("/signup")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
    SignUpResponse response = signUpService.signUp(request);
    return ApiResponse.success(response);
  }

  @PostMapping("/signin")
  public ApiResponse<TokenResponse> signIn(@Valid @RequestBody SignInRequest request) {
    TokenResponse response = signInService.signIn(request);
    return ApiResponse.success(response);
  }
}

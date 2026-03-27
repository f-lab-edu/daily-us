package com.jaeychoi.dailyus.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jaeychoi.dailyus.auth.dto.RefreshTokenRequest;
import com.jaeychoi.dailyus.auth.dto.SignInRequest;
import com.jaeychoi.dailyus.auth.dto.SignUpRequest;
import com.jaeychoi.dailyus.auth.dto.SignUpResponse;
import com.jaeychoi.dailyus.auth.dto.TokenResponse;
import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.auth.dto.LogoutRequest;
import com.jaeychoi.dailyus.auth.service.LogoutService;
import com.jaeychoi.dailyus.auth.service.RefreshTokenService;
import com.jaeychoi.dailyus.auth.service.SignInService;
import com.jaeychoi.dailyus.auth.service.SignUpService;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.exception.GlobalExceptionHandler;
import com.jaeychoi.dailyus.common.web.AuthRequestAttributes;
import com.jaeychoi.dailyus.common.web.AuthenticatedUserArgumentResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private SignUpService signUpService;

  @Mock
  private SignInService signInService;

  @Mock
  private RefreshTokenService refreshTokenService;

  @Mock
  private LogoutService logoutService;

  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.standaloneSetup(
            new AuthController(signUpService, signInService, refreshTokenService, logoutService))
        .setControllerAdvice(new GlobalExceptionHandler())
        .setCustomArgumentResolvers(new AuthenticatedUserArgumentResolver())
        .setValidator(validator)
        .setMessageConverters(new JacksonJsonHttpMessageConverter())
        .build();
    objectMapper = new ObjectMapper();
  }

  @Test
  void signUpReturnsCreatedResponse() throws Exception {
    // given
    SignUpRequest request = new SignUpRequest("tester@example.com", "Password1!", "tester");
    SignUpResponse response = new SignUpResponse(1L, request.email(), request.nickname());
    when(signUpService.signUp(any(SignUpRequest.class))).thenReturn(response);

    // when
    // then
    mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.message").doesNotExist())
        .andExpect(jsonPath("$.data.userId").value(1L))
        .andExpect(jsonPath("$.data.email").value(request.email()))
        .andExpect(jsonPath("$.data.nickname").value(request.nickname()));
  }

  @Test
  void signUpReturnsConflictWhenEmailAlreadyExists() throws Exception {
    // given
    SignUpRequest request = new SignUpRequest("tester@example.com", "Password1!", "tester");
    when(signUpService.signUp(any(SignUpRequest.class)))
        .thenThrow(new BaseException(ErrorCode.EMAIL_ALREADY_EXISTS));

    // when
    // then
    mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCode.EMAIL_ALREADY_EXISTS.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.EMAIL_ALREADY_EXISTS.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void signUpReturnsBadRequestWhenRequestBodyValidationFails() throws Exception {
    // given
    SignUpRequest request = new SignUpRequest("invalid-email", "password", "");

    // when
    // then
    mockMvc.perform(post("/api/v1/auth/signup")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.message").isNotEmpty())
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void signInReturnsAccessToken() throws Exception {
    // given
    SignInRequest request = new SignInRequest("tester@example.com", "Password1!");
    TokenResponse response = new TokenResponse(
        "access-token",
        "refresh-token",
        3600L,
        1209600L
    );
    when(signInService.signIn(any(SignInRequest.class))).thenReturn(response);

    // when
    // then
    mockMvc.perform(post("/api/v1/auth/signin")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.accessToken").value("access-token"))
        .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
        .andExpect(jsonPath("$.data.accessTokenExpiresIn").value(3600L))
        .andExpect(jsonPath("$.data.refreshTokenExpiresIn").value(1209600L));
  }

  @Test
  void refreshReturnsNewTokenPair() throws Exception {
    // given
    RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
    TokenResponse response = new TokenResponse(
        "new-access-token",
        "new-refresh-token",
        3600L,
        1209600L
    );
    when(refreshTokenService.refresh(any(RefreshTokenRequest.class))).thenReturn(response);

    // when
    // then
    mockMvc.perform(post("/api/v1/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
        .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"));
  }

  @Test
  void logoutReturnsOk() throws Exception {
    LogoutRequest request = new LogoutRequest("refresh-token");

    mockMvc.perform(post("/api/v1/auth/logout")
            .requestAttr(
                AuthRequestAttributes.CURRENT_USER,
                new CurrentUser(1L, "tester@example.com", "tester"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("OK"))
        .andExpect(jsonPath("$.message").doesNotExist())
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void logoutReturnsUnauthorizedWhenCurrentUserMissing() throws Exception {
    LogoutRequest request = new LogoutRequest("refresh-token");

    mockMvc.perform(post("/api/v1/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(ErrorCode.UNAUTHORIZED.getCode()))
        .andExpect(jsonPath("$.message").value(ErrorCode.UNAUTHORIZED.getMessage()))
        .andExpect(jsonPath("$.data").doesNotExist());
  }
}

package com.jaeychoi.dailyus.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jaeychoi.dailyus.auth.dto.SignUpRequest;
import com.jaeychoi.dailyus.auth.dto.SignUpResponse;
import com.jaeychoi.dailyus.auth.service.SignUpService;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.exception.GlobalExceptionHandler;
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

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(signUpService))
        .setControllerAdvice(new GlobalExceptionHandler())
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
}

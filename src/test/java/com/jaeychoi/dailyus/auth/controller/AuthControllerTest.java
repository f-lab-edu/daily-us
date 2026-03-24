package com.jaeychoi.dailyus.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.jaeychoi.dailyus.auth.dto.SignUpResponse;
import com.jaeychoi.dailyus.auth.service.SignUpService;
import com.jaeychoi.dailyus.common.fixture.UserFixture;
import com.jaeychoi.dailyus.common.support.ControllerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;

class AuthControllerTest extends ControllerTestSupport {

  @Mock
  private SignUpService signUpService;

  private AuthController authController;

  @BeforeEach
  void setUp() {
    authController = new AuthController(signUpService);
  }

  @Test
  void signUpReturnsCreatedResponse() {
    UserFixture userFixture = UserFixture.user();
    given(signUpService.signUp(userFixture.toSignUpRequest())).willReturn(
        userFixture.toSignUpResponse());

    ResponseEntity<SignUpResponse> response = authController.signUp(userFixture.toSignUpRequest());

    assertCreated(response);
    assertThat(response.getBody()).isEqualTo(userFixture.toSignUpResponse());
  }
}

package com.jaeychoi.dailyus.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.jaeychoi.dailyus.auth.dto.SignUpResponse;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.fixture.UserFixture;
import com.jaeychoi.dailyus.common.support.ServiceTestSupport;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.crypto.password.PasswordEncoder;

class SignUpServiceTest extends ServiceTestSupport {

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private SignUpService signUpService;

  @Captor
  private ArgumentCaptor<User> userCaptor;

  @Test
  void signUpCreatesUserWhenEmailAndNicknameAreAvailable() {
    UserFixture userFixture = UserFixture.user();
    given(userMapper.existsActiveByEmail(userFixture.email())).willReturn(false);
    given(userMapper.existsActiveByNickname(userFixture.nickname())).willReturn(false);
    given(passwordEncoder.encode(userFixture.password())).willReturn(userFixture.encodedPassword());
    doAnswer(invocation -> {
      User user = invocation.getArgument(0);
      userFixture.assignGeneratedId(user);
      return 1;
    }).when(userMapper).insert(any(User.class));

    SignUpResponse response = signUpService.signUp(userFixture.toSignUpRequest());

    verify(userMapper).insert(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    assertThat(savedUser.getEmail()).isEqualTo(userFixture.email());
    assertThat(savedUser.getNickname()).isEqualTo(userFixture.nickname());
    assertThat(savedUser.getPassword()).isEqualTo(userFixture.encodedPassword());
    assertThat(response).isEqualTo(userFixture.toSignUpResponse());
  }

  @Test
  void signUpThrowsExceptionWhenEmailAlreadyExists() {
    UserFixture userFixture = UserFixture.user();
    given(userMapper.existsActiveByEmail(userFixture.email())).willReturn(true);

    assertThatThrownBy(() -> signUpService.signUp(userFixture.toSignUpRequest()))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
  }

  @Test
  void signUpThrowsExceptionWhenNicknameAlreadyExists() {
    UserFixture userFixture = UserFixture.user();
    given(userMapper.existsActiveByEmail(userFixture.email())).willReturn(false);
    given(userMapper.existsActiveByNickname(userFixture.nickname())).willReturn(true);

    assertThatThrownBy(() -> signUpService.signUp(userFixture.toSignUpRequest()))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS);
  }
}

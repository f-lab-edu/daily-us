package com.jaeychoi.dailyus.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.auth.dto.SignUpRequest;
import com.jaeychoi.dailyus.auth.dto.SignUpResponse;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SignUpServiceTest {

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private SignUpService signUpService;

  @Test
  void signUpCreatesUserWithEncodedPassword() {
    // given
    SignUpRequest request = new SignUpRequest("tester@example.com", "Password1!", "tester");
    when(userMapper.existsActiveByEmail(request.email())).thenReturn(false);
    when(userMapper.existsActiveByNickname(request.nickname())).thenReturn(false);
    when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
    doAnswer(invocation -> {
      User user = invocation.getArgument(0);
      user.setUserId(1L);
      return null;
    }).when(userMapper).insert(any(User.class));

    // when
    SignUpResponse response = signUpService.signUp(request);

    // then
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    verify(userMapper).insert(userCaptor.capture());
    User savedUser = userCaptor.getValue();

    assertThat(savedUser.getEmail()).isEqualTo(request.email());
    assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
    assertThat(savedUser.getNickname()).isEqualTo(request.nickname());
    assertThat(response.userId()).isEqualTo(1L);
    assertThat(response.email()).isEqualTo(request.email());
    assertThat(response.nickname()).isEqualTo(request.nickname());
  }

  @Test
  void signUpThrowsWhenEmailAlreadyExists() {
    // given
    SignUpRequest request = new SignUpRequest("tester@example.com", "Password1!", "tester");
    when(userMapper.existsActiveByEmail(request.email())).thenReturn(true);

    // when
    // then
    assertThatThrownBy(() -> signUpService.signUp(request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

    verify(userMapper, never()).existsActiveByNickname(any());
    verify(passwordEncoder, never()).encode(any());
    verify(userMapper, never()).insert(any(User.class));
  }

  @Test
  void signUpThrowsWhenNicknameAlreadyExists() {
    // given
    SignUpRequest request = new SignUpRequest("tester@example.com", "Password1!", "tester");
    when(userMapper.existsActiveByEmail(request.email())).thenReturn(false);
    when(userMapper.existsActiveByNickname(request.nickname())).thenReturn(true);

    // when
    // then
    assertThatThrownBy(() -> signUpService.signUp(request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS);

    verify(passwordEncoder, never()).encode(any());
    verify(userMapper, never()).insert(any(User.class));
  }
}

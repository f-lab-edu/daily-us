package com.jaeychoi.dailyus.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.auth.repository.RefreshTokenRepository;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.jwt.AccessTokenDetails;
import com.jaeychoi.dailyus.common.jwt.JwtTokenProvider;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserWithdrawServiceTest {

  @Mock
  private UserMapper userMapper;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @InjectMocks
  private UserWithdrawService userWithdrawService;

  @Test
  void withdrawUpdatesIdentityAndDeletesRefreshToken() {
    User user = User.builder()
        .userId(1L)
        .email("abc@google.com")
        .nickname("dailyus")
        .build();
    AccessTokenDetails accessTokenDetails = new AccessTokenDetails(
        new com.jaeychoi.dailyus.auth.domain.CurrentUser(1L, "abc@google.com", "dailyus"),
        "access-token-id",
        Instant.parse("2026-06-18T00:00:00Z")
    );
    when(userMapper.findActiveById(1L)).thenReturn(user);
    when(userMapper.withdraw(any(User.class))).thenReturn(1);
    when(jwtTokenProvider.parseAccessTokenDetails("access-token")).thenReturn(accessTokenDetails);

    userWithdrawService.withdraw(1L, "access-token");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userMapper).withdraw(captor.capture());
    User withdrawnUser = captor.getValue();

    org.assertj.core.api.Assertions.assertThat(withdrawnUser.getEmail())
        .isEqualTo("withdrawn+1+abc@google.com");
    org.assertj.core.api.Assertions.assertThat(withdrawnUser.getNickname())
        .isEqualTo("withdrawn-1-dailyus");
    org.assertj.core.api.Assertions.assertThat(withdrawnUser.getDeletedAt()).isNotNull();
    verify(refreshTokenRepository).blacklistAccessToken(accessTokenDetails);
    verify(refreshTokenRepository).delete(1L);
  }

  @Test
  void withdrawThrowsWhenUserDoesNotExist() {
    when(userMapper.findActiveById(1L)).thenReturn(null);

    assertThatThrownBy(() -> userWithdrawService.withdraw(1L, "access-token"))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);

    verify(userMapper, never()).withdraw(any(User.class));
    verify(refreshTokenRepository, never()).delete(1L);
    verify(jwtTokenProvider, never()).parseAccessTokenDetails(anyString());
  }

  @Test
  void withdrawThrowsWhenUpdateDoesNotAffectAnyRows() {
    User user = User.builder()
        .userId(1L)
        .email("abc@google.com")
        .nickname("dailyus")
        .build();
    when(userMapper.findActiveById(1L)).thenReturn(user);
    when(userMapper.withdraw(any(User.class))).thenReturn(0);

    assertThatThrownBy(() -> userWithdrawService.withdraw(1L, "access-token"))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);

    verify(refreshTokenRepository, never()).delete(1L);
    verify(refreshTokenRepository, never()).blacklistAccessToken(any());
  }
}

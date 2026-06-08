package com.jaeychoi.dailyus.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.auth.repository.RefreshTokenRepository;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserWithdrawServiceTest {

  @Mock
  private UserMapper userMapper;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks
  private UserWithdrawService userWithdrawService;

  @Test
  void withdrawDeletesUserAndRefreshToken() {
    when(userMapper.existsActiveById(1L)).thenReturn(true);
    when(userMapper.delete(1L)).thenReturn(1);

    userWithdrawService.withdraw(1L);

    verify(userMapper).delete(1L);
    verify(refreshTokenRepository).delete(1L);
  }

  @Test
  void withdrawThrowsWhenUserDoesNotExist() {
    when(userMapper.existsActiveById(1L)).thenReturn(false);

    assertThatThrownBy(() -> userWithdrawService.withdraw(1L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);

    verify(userMapper, never()).delete(1L);
    verify(refreshTokenRepository, never()).delete(1L);
  }

  @Test
  void withdrawThrowsWhenDeleteDoesNotAffectAnyRows() {
    when(userMapper.existsActiveById(1L)).thenReturn(true);
    when(userMapper.delete(1L)).thenReturn(0);

    assertThatThrownBy(() -> userWithdrawService.withdraw(1L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);

    verify(refreshTokenRepository, never()).delete(1L);
  }
}

package com.jaeychoi.dailyus.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.auth.dto.LogoutRequest;
import com.jaeychoi.dailyus.auth.repository.RefreshTokenRepository;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.jwt.JwtTokenProvider;
import com.jaeychoi.dailyus.common.jwt.RefreshTokenDetails;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LogoutServiceTest {

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks
  private LogoutService logoutService;

  @Test
  void logoutRevokesRefreshTokenWhenUserMatches() {
    CurrentUser currentUser = new CurrentUser(1L, "tester@example.com", "tester");
    LogoutRequest request = new LogoutRequest("refresh-token");
    RefreshTokenDetails refreshTokenDetails = new RefreshTokenDetails(
        currentUser,
        "token-id",
        Instant.parse("2026-04-10T00:00:00Z")
    );

    when(jwtTokenProvider.parseRefreshTokenDetails(request.refreshToken()))
        .thenReturn(refreshTokenDetails);
    when(refreshTokenRepository.revoke(1L, refreshTokenDetails)).thenReturn(true);

    logoutService.logout(currentUser, request);

    verify(refreshTokenRepository).revoke(1L, refreshTokenDetails);
  }

  @Test
  void logoutThrowsWhenRefreshTokenBelongsToAnotherUser() {
    CurrentUser currentUser = new CurrentUser(1L, "tester@example.com", "tester");
    LogoutRequest request = new LogoutRequest("refresh-token");
    RefreshTokenDetails refreshTokenDetails = new RefreshTokenDetails(
        new CurrentUser(2L, "other@example.com", "other"),
        "token-id",
        Instant.parse("2026-04-10T00:00:00Z")
    );

    when(jwtTokenProvider.parseRefreshTokenDetails(request.refreshToken()))
        .thenReturn(refreshTokenDetails);

    assertThatThrownBy(() -> logoutService.logout(currentUser, request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
  }

  @Test
  void logoutThrowsWhenStoredSessionDoesNotMatch() {
    CurrentUser currentUser = new CurrentUser(1L, "tester@example.com", "tester");
    LogoutRequest request = new LogoutRequest("refresh-token");
    RefreshTokenDetails refreshTokenDetails = new RefreshTokenDetails(
        currentUser,
        "token-id",
        Instant.parse("2026-04-10T00:00:00Z")
    );

    when(jwtTokenProvider.parseRefreshTokenDetails(request.refreshToken()))
        .thenReturn(refreshTokenDetails);
    when(refreshTokenRepository.revoke(1L, refreshTokenDetails)).thenReturn(false);

    assertThatThrownBy(() -> logoutService.logout(currentUser, request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
  }
}

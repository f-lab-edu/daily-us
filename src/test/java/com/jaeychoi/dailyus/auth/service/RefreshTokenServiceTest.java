package com.jaeychoi.dailyus.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.auth.dto.RefreshTokenRequest;
import com.jaeychoi.dailyus.auth.dto.TokenResponse;
import com.jaeychoi.dailyus.auth.repository.RefreshTokenRepository;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.common.jwt.JwtTokenPair;
import com.jaeychoi.dailyus.common.jwt.JwtTokenProvider;
import com.jaeychoi.dailyus.common.jwt.RefreshTokenDetails;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  @Mock
  private UserMapper userMapper;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks
  private RefreshTokenService refreshTokenService;

  @Test
  void refreshRotatesRefreshTokenWhenStoredSessionMatches() {
    RefreshTokenRequest request = new RefreshTokenRequest("current-refresh-token");
    RefreshTokenDetails currentRefreshToken = new RefreshTokenDetails(
        new CurrentUser(1L, "tester@example.com", "tester"),
        "current-token-id",
        Instant.parse("2026-03-27T00:00:00Z")
    );
    User user = User.builder()
        .userId(1L)
        .email("tester@example.com")
        .nickname("tester")
        .password("encoded-password")
        .build();
    JwtTokenPair newTokenPair = new JwtTokenPair(
        "new-access-token",
        "new-refresh-token",
        3600L,
        1209600L
    );
    RefreshTokenDetails newRefreshToken = new RefreshTokenDetails(
        new CurrentUser(1L, "tester@example.com", "tester"),
        "new-token-id",
        Instant.parse("2026-04-10T00:00:00Z")
    );

    when(jwtTokenProvider.parseRefreshTokenDetails(request.refreshToken()))
        .thenReturn(currentRefreshToken);
    when(refreshTokenRepository.isBlacklisted("current-token-id")).thenReturn(false);
    when(userMapper.findActiveById(1L)).thenReturn(user);
    when(jwtTokenProvider.createTokenPair(user)).thenReturn(newTokenPair);
    when(jwtTokenProvider.parseRefreshTokenDetails(newTokenPair.refreshToken()))
        .thenReturn(newRefreshToken);
    when(refreshTokenRepository.rotate(1L, currentRefreshToken, newRefreshToken)).thenReturn(true);

    TokenResponse response = refreshTokenService.refresh(request);

    assertThat(response.accessToken()).isEqualTo("new-access-token");
    assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
  }

  @Test
  void refreshThrowsWhenStoredSessionDoesNotMatch() {
    RefreshTokenRequest request = new RefreshTokenRequest("current-refresh-token");
    RefreshTokenDetails currentRefreshToken = new RefreshTokenDetails(
        new CurrentUser(1L, "tester@example.com", "tester"),
        "current-token-id",
        Instant.parse("2026-03-27T00:00:00Z")
    );

    when(jwtTokenProvider.parseRefreshTokenDetails(request.refreshToken()))
        .thenReturn(currentRefreshToken);
    when(refreshTokenRepository.isBlacklisted("current-token-id")).thenReturn(true);

    assertThatThrownBy(() -> refreshTokenService.refresh(request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);

    verify(userMapper, never()).findActiveById(any());
    verify(refreshTokenRepository, never()).rotate(any(), any(), any());
  }

  @Test
  void refreshThrowsWhenTokenWasAlreadyRotated() {
    RefreshTokenRequest request = new RefreshTokenRequest("current-refresh-token");
    RefreshTokenDetails currentRefreshToken = new RefreshTokenDetails(
        new CurrentUser(1L, "tester@example.com", "tester"),
        "current-token-id",
        Instant.parse("2026-03-27T00:00:00Z")
    );
    User user = User.builder()
        .userId(1L)
        .email("tester@example.com")
        .nickname("tester")
        .password("encoded-password")
        .build();
    JwtTokenPair newTokenPair = new JwtTokenPair(
        "new-access-token",
        "new-refresh-token",
        3600L,
        1209600L
    );
    RefreshTokenDetails newRefreshToken = new RefreshTokenDetails(
        new CurrentUser(1L, "tester@example.com", "tester"),
        "new-token-id",
        Instant.parse("2026-04-10T00:00:00Z")
    );

    when(jwtTokenProvider.parseRefreshTokenDetails(request.refreshToken()))
        .thenReturn(currentRefreshToken);
    when(refreshTokenRepository.isBlacklisted("current-token-id")).thenReturn(false);
    when(userMapper.findActiveById(1L)).thenReturn(user);
    when(jwtTokenProvider.createTokenPair(user)).thenReturn(newTokenPair);
    when(jwtTokenProvider.parseRefreshTokenDetails(newTokenPair.refreshToken()))
        .thenReturn(newRefreshToken);
    when(refreshTokenRepository.rotate(1L, currentRefreshToken, newRefreshToken)).thenReturn(false);

    assertThatThrownBy(() -> refreshTokenService.refresh(request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
  }
}

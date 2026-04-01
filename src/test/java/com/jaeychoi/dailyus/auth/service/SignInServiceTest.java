package com.jaeychoi.dailyus.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.auth.dto.SignInRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SignInServiceTest {

  @Mock
  private UserMapper userMapper;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks
  private SignInService signInService;

  @Test
  void signInReturnsTokenResponseWhenCredentialsAreValid() {
    // given
    SignInRequest request = new SignInRequest("tester@example.com", "Password1!");
    User user = User.builder()
        .userId(1L)
        .email(request.email())
        .password("encoded-password")
        .nickname("tester")
        .build();
    JwtTokenPair tokenPair = new JwtTokenPair("access-token", "refresh-token", 3600L, 1209600L);
    RefreshTokenDetails refreshTokenDetails = new RefreshTokenDetails(
        new CurrentUser(1L, request.email(), "tester"),
        "refresh-token-id",
        Instant.parse("2026-03-27T00:00:00Z")
    );

    when(userMapper.findActiveByEmail(request.email())).thenReturn(user);
    when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
    when(jwtTokenProvider.createTokenPair(user)).thenReturn(tokenPair);
    when(jwtTokenProvider.parseRefreshTokenDetails(tokenPair.refreshToken()))
        .thenReturn(refreshTokenDetails);

    // when
    TokenResponse response = signInService.signIn(request);

    // then
    assertThat(response.accessToken()).isEqualTo(tokenPair.accessToken());
    assertThat(response.refreshToken()).isEqualTo(tokenPair.refreshToken());
    assertThat(response.accessTokenExpiresIn()).isEqualTo(tokenPair.accessTokenExpiresIn());
    assertThat(response.refreshTokenExpiresIn()).isEqualTo(tokenPair.refreshTokenExpiresIn());
    verify(refreshTokenRepository).save(user.getUserId(), refreshTokenDetails);
  }

  @Test
  void signInThrowsWhenUserDoesNotExist() {
    // given
    SignInRequest request = new SignInRequest("tester@example.com", "Password1!");
    when(userMapper.findActiveByEmail(request.email())).thenReturn(null);

    // when
    // then
    assertThatThrownBy(() -> signInService.signIn(request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

    verify(passwordEncoder, never()).matches(any(), any());
    verify(jwtTokenProvider, never()).createTokenPair(any(User.class));
    verify(refreshTokenRepository, never()).save(any(), any());
  }

  @Test
  void signInThrowsWhenPasswordDoesNotMatch() {
    // given
    SignInRequest request = new SignInRequest("tester@example.com", "Password1!");
    User user = User.builder()
        .userId(1L)
        .email(request.email())
        .password("encoded-password")
        .nickname("tester")
        .build();

    when(userMapper.findActiveByEmail(request.email())).thenReturn(user);
    when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

    // when
    // then
    assertThatThrownBy(() -> signInService.signIn(request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_CREDENTIALS);

    verify(jwtTokenProvider, never()).createTokenPair(any(User.class));
    verify(refreshTokenRepository, never()).save(any(), any());
  }
}

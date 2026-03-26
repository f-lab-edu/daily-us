package com.jaeychoi.dailyus.common.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private static final JwtProperties JWT_PROPERTIES = new JwtProperties(
      "daily-us-test",
      "test-secret-key-that-is-long-enough-for-hmac-256",
      3600L,
      1209600L
  );

  private JwtTokenProvider jwtTokenProvider;

  @BeforeEach
  void setUp() {
    jwtTokenProvider = new JwtTokenProvider(JWT_PROPERTIES);
  }

  @Test
  void createAccessTokenAndParseAccessToken() {
    User user = testUser();

    String token = jwtTokenProvider.createAccessToken(user);
    CurrentUser currentUser = jwtTokenProvider.parseAccessToken(token);

    assertNotNull(token);
    assertEquals(user.getUserId(), currentUser.userId());
    assertEquals(user.getEmail(), currentUser.email());
    assertEquals(user.getNickname(), currentUser.nickname());
  }

  @Test
  void createRefreshTokenAndParseRefreshToken() {
    User user = testUser();

    String token = jwtTokenProvider.createRefreshToken(user);
    CurrentUser currentUser = jwtTokenProvider.parseRefreshToken(token);

    assertNotNull(token);
    assertEquals(user.getUserId(), currentUser.userId());
    assertEquals(user.getEmail(), currentUser.email());
    assertEquals(user.getNickname(), currentUser.nickname());
  }

  @Test
  void createTokenPairReturnsConfiguredExpirations() {
    JwtTokenPair tokenPair = jwtTokenProvider.createTokenPair(testUser());

    assertNotNull(tokenPair.accessToken());
    assertNotNull(tokenPair.refreshToken());
    assertEquals(JWT_PROPERTIES.accessTokenExpirationSeconds(), tokenPair.accessTokenExpiresIn());
    assertEquals(JWT_PROPERTIES.refreshTokenExpirationSeconds(), tokenPair.refreshTokenExpiresIn());
  }

  @Test
  void parseAccessTokenThrowsWhenRefreshTokenIsProvided() {
    String refreshToken = jwtTokenProvider.createRefreshToken(testUser());

    BaseException exception = assertThrows(
        BaseException.class,
        () -> jwtTokenProvider.parseAccessToken(refreshToken)
    );

    assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
  }

  @Test
  void parseRefreshTokenThrowsWhenAccessTokenIsProvided() {
    String accessToken = jwtTokenProvider.createAccessToken(testUser());

    BaseException exception = assertThrows(
        BaseException.class,
        () -> jwtTokenProvider.parseRefreshToken(accessToken)
    );

    assertEquals(ErrorCode.INVALID_REFRESH_TOKEN, exception.getErrorCode());
  }

  @Test
  void parseAccessTokenThrowsWhenTokenIsInvalid() {
    BaseException exception = assertThrows(
        BaseException.class,
        () -> jwtTokenProvider.parseAccessToken("invalid-token")
    );

    assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
  }

  private User testUser() {
    return User.builder()
        .userId(1L)
        .email("tester@example.com")
        .nickname("tester")
        .password("encoded-password")
        .build();
  }
}

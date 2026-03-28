package com.jaeychoi.dailyus.common.jwt;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

  private final JwtProperties jwtProperties;
  private final SecretKey secretKey;

  public JwtTokenProvider(JwtProperties jwtProperties) {
    this.jwtProperties = jwtProperties;
    this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
  }

  public String createAccessToken(User user) {
    return createToken(user, JwtTokenType.ACCESS, jwtProperties.accessTokenExpirationSeconds());
  }

  public String createRefreshToken(User user) {
    return createToken(user, JwtTokenType.REFRESH, jwtProperties.refreshTokenExpirationSeconds());
  }

  public JwtTokenPair createTokenPair(User user) {
    return new JwtTokenPair(
        createAccessToken(user),
        createRefreshToken(user),
        jwtProperties.accessTokenExpirationSeconds(),
        jwtProperties.refreshTokenExpirationSeconds()
    );
  }

  public CurrentUser parseAccessToken(String token) {
    return parse(token, JwtTokenType.ACCESS, ErrorCode.INVALID_TOKEN);
  }

  public CurrentUser parseRefreshToken(String token) {
    return parse(token, JwtTokenType.REFRESH, ErrorCode.INVALID_REFRESH_TOKEN);
  }

  private String createToken(User user, JwtTokenType tokenType, long expirationSeconds) {
    JwtUserClaims userClaims = JwtUserClaims.from(user);
    Instant now = Instant.now();
    Instant expiration = now.plusSeconds(expirationSeconds);

    return Jwts.builder()
        .issuer(jwtProperties.issuer())
        .subject(String.valueOf(user.getUserId()))
        .claims(userClaims.toMap())
        .claim(JwtClaimNames.TOKEN_TYPE, tokenType.name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiration))
        .signWith(secretKey)
        .compact();
  }

  private CurrentUser parse(String token, JwtTokenType expectedType, ErrorCode errorCode) {
    try {
      Claims claims = Jwts.parser()
          .verifyWith(secretKey)
          .build()
          .parseSignedClaims(token)
          .getPayload();

      JwtTokenType actualType = JwtTokenType.valueOf(
          claims.get(JwtClaimNames.TOKEN_TYPE, String.class));
      if (actualType != expectedType) {
        throw new BaseException(errorCode);
      }

      return JwtUserClaims.from(claims).toCurrentUser(Long.valueOf(claims.getSubject()));
    } catch (JwtException | IllegalArgumentException e) {
      throw new BaseException(errorCode);
    }
  }

  public long getAccessTokenExpirationSeconds() {
    return jwtProperties.accessTokenExpirationSeconds();
  }

  public long getRefreshTokenExpirationSeconds() {
    return jwtProperties.refreshTokenExpirationSeconds();
  }
}

package com.jaeychoi.dailyus.common.jwt;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import com.jaeychoi.dailyus.user.domain.User;
import io.jsonwebtoken.Claims;
import java.util.Map;

public record JwtUserClaims(String email, String nickname) {

  public static JwtUserClaims from(User user) {
    return new JwtUserClaims(user.getEmail(), user.getNickname());
  }

  public static JwtUserClaims from(Claims claims) {
    return new JwtUserClaims(
        claims.get(JwtClaimNames.EMAIL, String.class),
        claims.get(JwtClaimNames.NICKNAME, String.class)
    );
  }

  public Map<String, Object> toMap() {
    return Map.of(
        JwtClaimNames.EMAIL, email,
        JwtClaimNames.NICKNAME, nickname
    );
  }

  public CurrentUser toCurrentUser(Long userId) {
    return new CurrentUser(userId, email, nickname);
  }
}

package com.jaeychoi.dailyus.common.jwt;

public record JwtTokenPair(
    String accessToken,
    String refreshToken,
    long accessTokenExpiresIn,
    long refreshTokenExpiresIn
) {
}

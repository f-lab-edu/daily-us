package com.jaeychoi.dailyus.auth.dto;

public record TokenResponse(
    String accessToken,
    String refreshToken,
    long accessTokenExpiresIn,
    long refreshTokenExpiresIn
) {
}

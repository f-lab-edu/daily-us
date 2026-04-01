package com.jaeychoi.dailyus.common.jwt;

import com.jaeychoi.dailyus.auth.domain.CurrentUser;
import java.time.Instant;

public record RefreshTokenDetails(
    CurrentUser user,
    String tokenId,
    Instant expiresAt
) {
}

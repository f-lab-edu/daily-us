package com.jaeychoi.dailyus.auth.repository;

import com.jaeychoi.dailyus.common.jwt.RefreshTokenDetails;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepository {

  private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>(
      """
      local activeKey = KEYS[1]
      local blacklistKey = KEYS[2]
      local currentTokenId = ARGV[1]
      local newTokenId = ARGV[2]
      local newActiveTtlMillis = tonumber(ARGV[3])
      local blacklistTtlMillis = tonumber(ARGV[4])
      local activeTokenId = redis.call('GET', activeKey)

      if not activeTokenId or activeTokenId ~= currentTokenId then
        return 0
      end

      redis.call('SET', activeKey, newTokenId, 'PX', newActiveTtlMillis)

      if blacklistTtlMillis > 0 then
        redis.call('SET', blacklistKey, '1', 'PX', blacklistTtlMillis)
      end

      return 1
      """,
      Long.class
  );

  private final StringRedisTemplate redisTemplate;

  public RefreshTokenRepository(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void save(Long userId, RefreshTokenDetails refreshTokenDetails) {
    redisTemplate.opsForValue().set(
        activeKey(userId),
        refreshTokenDetails.tokenId(),
        ttlUntil(refreshTokenDetails.expiresAt())
    );
  }

  public boolean rotate(Long userId, RefreshTokenDetails currentToken, RefreshTokenDetails newToken) {
    Long updated = redisTemplate.execute(
        ROTATE_SCRIPT,
        List.of(activeKey(userId), blacklistKey(currentToken.tokenId())),
        currentToken.tokenId(),
        newToken.tokenId(),
        String.valueOf(ttlUntil(newToken.expiresAt()).toMillis()),
        String.valueOf(ttlUntil(currentToken.expiresAt()).toMillis())
    );
    return updated != null && updated == 1L;
  }

  public boolean isBlacklisted(String tokenId) {
    Boolean hasKey = redisTemplate.hasKey(blacklistKey(tokenId));
    return Boolean.TRUE.equals(hasKey);
  }

  private String activeKey(Long userId) {
    return "auth:refresh:active:" + userId;
  }

  private String blacklistKey(String tokenId) {
    return "auth:refresh:blacklist:" + tokenId;
  }

  private Duration ttlUntil(Instant expiresAt) {
    long ttlMillis = Duration.between(Instant.now(), expiresAt).toMillis();
    return Duration.ofMillis(Math.max(ttlMillis, 1L));
  }
}

package com.jaeychoi.dailyus.post.repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class PostFeedRepository {

  private static final String KEY_PREFIX = "feed:user:";
  private static final int MAX_CACHED_POST_COUNT = 100;
  private static final DefaultRedisScript<Long> ADD_AND_TRIM_SCRIPT = new DefaultRedisScript<>(
      """
          local member = ARGV[1]
          local score = tonumber(ARGV[2])
          local maxCount = tonumber(ARGV[3])
          
          if member == false or score == nil or maxCount == nil then
            return 0
          end
          
          for i, key in ipairs(KEYS) do
            redis.call('ZADD', key, score, member)
          
            local size = redis.call('ZCARD', key)
            if size > maxCount then
              redis.call('ZREMRANGEBYRANK', key, 0, size - maxCount - 1)
            end
          end
          
          return #KEYS
          """,
      Long.class
  );
  private static final DefaultRedisScript<List> FIND_BY_CURSOR_SCRIPT = new DefaultRedisScript<>(
      """
          local key = KEYS[1]
          local cursorMember = ARGV[1]
          local pageSize = tonumber(ARGV[2])
          
          local start = 0
          if cursorMember ~= '' then
            local rank = redis.call('ZREVRANK', key, cursorMember)
            if not rank then
              return nil
            end
            start = rank + 1
          end
          
          return redis.call('ZREVRANGE', key, start, start + pageSize - 1)
          """,
      List.class
  );

  private final StringRedisTemplate redisTemplate;

  public PostFeedRepository(
      StringRedisTemplate redisTemplate
  ) {
    this.redisTemplate = redisTemplate;
  }

  public List<Long> findByUserIdAndCursor(
      Long userId,
      Long cursor,
      long size
  ) {
    if (userId == null || size <= 0) {
      return null;
    }

    Object cached = redisTemplate.execute(
        FIND_BY_CURSOR_SCRIPT,
        List.of(buildKey(userId)),
        resolveCursor(cursor),
        String.valueOf(size)
    );

    if (!(cached instanceof List<?> cachedPosts) || cachedPosts.isEmpty()) {
      return null;
    }

    List<Long> postIds = new ArrayList<>();
    for (Object cachedPost : cachedPosts) {
      Long postId = toLongOrNull(cachedPost);
      if (postId != null) {
        postIds.add(postId);
      }
    }

    return postIds.isEmpty() ? null : postIds;
  }

  public void addPostIdToFeeds(List<Long> userIds, Long postId, LocalDateTime createdAt) {
    if (userIds == null || userIds.isEmpty() || postId == null || createdAt == null) {
      return;
    }

    List<String> keys = userIds.stream()
        .filter(Objects::nonNull)
        .map(this::buildKey)
        .toList();

    if (keys.isEmpty()) {
      return;
    }

    redisTemplate.execute(
        ADD_AND_TRIM_SCRIPT,
        keys,
        String.valueOf(postId),
        String.valueOf(toEpochMilli(createdAt)),
        String.valueOf(MAX_CACHED_POST_COUNT)
    );
  }

  private String buildKey(Long userId) {
    return KEY_PREFIX + userId;
  }

  private String resolveCursor(Long cursor) {
    return cursor == null ? "" : String.valueOf(cursor);
  }

  private Long toLongOrNull(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String stringValue) {
      if (stringValue.isBlank() || "null".equalsIgnoreCase(stringValue)) {
        return null;
      }
      return Long.parseLong(stringValue);
    }
    if (value instanceof byte[] byteArrayValue) {
      return toLongOrNull(new String(byteArrayValue));
    }
    if (value instanceof Long longValue) {
      return longValue;
    }
    if (value instanceof Number numberValue) {
      return numberValue.longValue();
    }
    throw new TypeMismatchDataAccessException(
        "Cached post id type mismatch. valueType=" + value.getClass().getName());
  }

  private long toEpochMilli(LocalDateTime createdAt) {
    return createdAt.atZone(ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli();
  }
}

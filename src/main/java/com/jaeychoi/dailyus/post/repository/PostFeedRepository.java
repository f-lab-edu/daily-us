package com.jaeychoi.dailyus.post.repository;

import java.util.List;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
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
      RedisTemplate<String, Object> redisTemplate
  ) {
    this.redisTemplate = redisTemplate;
  }

  public List<Long> findByUserIdAndCursor(
      Long userId,
      Long cursor,
      long size
  ) {
    Object cached = redisTemplate.opsForValue().get(buildKey(userId));
    if (cached == null) {
      return null;
    }
    if (!(cached instanceof List)) {
      throw new TypeMismatchDataAccessException("Cached Object type mismatch.");
    }
    List<?> cachedPosts = (List<?>) cached;
    List<Long> postIds = cachedPosts.stream()
        .map(this::toLong)
        .toList();

    int startIndex = resolveStartIndex(postIds, cursor);
    if (startIndex < 0) {
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

  private int resolveStartIndex(List<Long> postIds, Long postId) {
    if (postId == null) {
      return 0;
    }

    int cursorIndex = postIds.indexOf(postId);
    if (cursorIndex < 0) {
      return -1;
    }

    return cursorIndex + 1;
  }

  private Long toLong(Object value) {
    if (value instanceof Long longValue) {
      return longValue;
    }
    if (value instanceof Number numberValue) {
      return numberValue.longValue();
    }
    throw new TypeMismatchDataAccessException("Cached post id type mismatch.");
  }
}

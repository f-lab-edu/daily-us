package com.jaeychoi.dailyus.post.repository;

import java.util.List;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PostFeedRepository {

  private static final String KEY_PREFIX = "feed:user:";

  private final RedisTemplate<String, Object> redisTemplate;

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

    int endIndex = Math.min(startIndex + Math.toIntExact(size), postIds.size());
    return postIds.subList(startIndex, endIndex);
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

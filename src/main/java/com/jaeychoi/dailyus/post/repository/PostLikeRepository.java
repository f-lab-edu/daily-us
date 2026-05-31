package com.jaeychoi.dailyus.post.repository;

import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

@Repository
public class PostLikeRepository {

  private static final String DELTA_KEY_PREFIX = "post:like:delta:";
  private static final String DIRTY_KEY = "post:like:dirty";
  private static final DefaultRedisScript<String> DRAIN_DELTA_SCRIPT = new DefaultRedisScript<>(
      """
          local value = redis.call('GET', KEYS[1])
          if value then
            redis.call('DEL', KEYS[1])
          end
          return value
          """,
      String.class
  );

  private final StringRedisTemplate redisTemplate;

  public PostLikeRepository(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public long incrementDelta(Long postId) {
    return addDelta(postId, 1L);
  }

  public long decrementDelta(Long postId) {
    return addDelta(postId, -1L);
  }

  public long addDelta(Long postId, long delta) {
    if (postId == null || delta == 0L) {
      return 0L;
    }

    Long updated = redisTemplate.opsForValue().increment(buildDeltaKey(postId), delta);
    redisTemplate.opsForSet().add(DIRTY_KEY, String.valueOf(postId));
    return updated == null ? 0L : updated;
  }

  public long findDelta(Long postId) {
    if (postId == null) {
      return 0L;
    }

    String value = redisTemplate.opsForValue().get(buildDeltaKey(postId));
    return value == null ? 0L : Long.parseLong(value);
  }

  public List<Long> popDirtyPostIds(long limit) {
    if (limit <= 0) {
      return List.of();
    }

    return redisTemplate.opsForSet().pop(DIRTY_KEY, limit)
        .stream()
        .map(Long::valueOf)
        .toList();
  }

  public long drainDelta(Long postId) {
    if (postId == null) {
      return 0L;
    }

    String drained = redisTemplate.execute(
        DRAIN_DELTA_SCRIPT,
        List.of(buildDeltaKey(postId))
    );
    return drained == null ? 0L : Long.parseLong(drained);
  }

  public void clear(Long postId) {
    if (postId == null) {
      return;
    }

    redisTemplate.delete(buildDeltaKey(postId));
    redisTemplate.opsForSet().remove(DIRTY_KEY, String.valueOf(postId));
  }

  private String buildDeltaKey(Long postId) {
    return DELTA_KEY_PREFIX + postId;
  }
}

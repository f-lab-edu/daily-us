package com.jaeychoi.dailyus.post.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class PostLikeRepositoryTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOperations;

  @Mock
  private SetOperations<String, String> setOperations;

  @InjectMocks
  private PostLikeRepository postLikeRepository;

  @Test
  void incrementDeltaStoresDirtyPostId() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(valueOperations.increment("post:like:delta:10", 1L)).thenReturn(2L);

    long delta = postLikeRepository.incrementDelta(10L);

    assertThat(delta).isEqualTo(2L);
    verify(setOperations).add("post:like:dirty", "10");
  }

  @Test
  void popDirtyPostIdsReturnsParsedIds() {
    when(redisTemplate.opsForSet()).thenReturn(setOperations);
    when(setOperations.pop("post:like:dirty", 5)).thenReturn(List.of("10", "11"));

    List<Long> postIds = postLikeRepository.popDirtyPostIds(5);

    assertThat(postIds).containsExactly(10L, 11L);
  }

  @Test
  void drainDeltaDeletesKeyAtomically() {
    when(redisTemplate.execute(
        any(RedisScript.class),
        eq(List.of("post:like:delta:10"))
    )).thenReturn("3");

    long delta = postLikeRepository.drainDelta(10L);

    assertThat(delta).isEqualTo(3L);
  }
}

package com.jaeychoi.dailyus.post.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class PostFeedRepositoryTest {

  @Mock
  private StringRedisTemplate redisTemplate;

  @InjectMocks
  private PostFeedRepository postFeedRepository;

  @Test
  void addPostIdToFeedsExecutesAtomicAddAndTrimScript() {
    postFeedRepository.addPostIdToFeeds(
        List.of(7L),
        101L,
        LocalDateTime.of(2026, 4, 26, 12, 0)
    );

    verify(redisTemplate).execute(
        any(RedisScript.class),
        eq(List.of("feed:user:7")),
        eq("101"),
        eq("1777172400000"),
        eq("100")
    );
  }

  @Test
  void findByUserIdAndCursorReturnsCachedSlice() {
    @SuppressWarnings("unchecked")
    List<Object> cachedPosts = (List<Object>) (List<?>) List.of(
        "8",
        "7"
    );
    org.mockito.Mockito.when(redisTemplate.execute(
        any(RedisScript.class),
        eq(List.of("feed:user:7")),
        eq("9"),
        eq("2")
    )).thenReturn(cachedPosts);

    List<Long> postIds = postFeedRepository.findByUserIdAndCursor(7L, 9L, 2L);

    assertThat(postIds).containsExactly(8L, 7L);
  }

  @Test
  void findByUserIdAndCursorTreatsNullCursorAsFirstPage() {
    @SuppressWarnings("unchecked")
    List<Object> cachedPosts = (List<Object>) (List<?>) List.of(
        "10",
        "9"
    );
    org.mockito.Mockito.when(redisTemplate.execute(
        any(RedisScript.class),
        eq(List.of("feed:user:7")),
        eq(""),
        eq("2")
    )).thenReturn(cachedPosts);

    List<Long> postIds = postFeedRepository.findByUserIdAndCursor(7L, null, 2L);

    assertThat(postIds).containsExactly(10L, 9L);
  }

  @Test
  void findByUserIdAndCursorTreatsInvalidCachedMembersAsCacheMiss() {
    @SuppressWarnings("unchecked")
    List<Object> cachedPosts = (List<Object>) (List<?>) List.of("null", "");
    org.mockito.Mockito.when(redisTemplate.execute(
        any(RedisScript.class),
        eq(List.of("feed:user:7")),
        eq(""),
        eq("2")
    )).thenReturn(cachedPosts);

    List<Long> postIds = postFeedRepository.findByUserIdAndCursor(7L, null, 2L);

    assertThat(postIds).isNull();
  }
}

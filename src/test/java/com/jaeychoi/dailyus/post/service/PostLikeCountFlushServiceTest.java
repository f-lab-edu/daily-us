package com.jaeychoi.dailyus.post.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.post.repository.PostLikeRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostLikeCountFlushServiceTest {

  @Mock
  private PostLikeRepository postLikeRepository;

  @Mock
  private PostMapper postMapper;

  @InjectMocks
  private PostLikeCountFlushService postLikeCountFlushService;

  @Test
  void flushBatchAppliesDrainedDeltasToPosts() {
    when(postLikeRepository.popDirtyPostIds(2L)).thenReturn(List.of(10L));
    when(postLikeRepository.drainDelta(10L)).thenReturn(3L);

    postLikeCountFlushService.flushBatch(2L);

    verify(postMapper).applyLikeCountDelta(10L, 3L);
  }

  @Test
  void flushBatchSkipsWhenDrainedDeltaIsZero() {
    when(postLikeRepository.popDirtyPostIds(2L)).thenReturn(List.of(10L));
    when(postLikeRepository.drainDelta(10L)).thenReturn(0L);

    postLikeCountFlushService.flushBatch(2L);

    verify(postMapper, never()).applyLikeCountDelta(10L, 0L);
  }

  @Test
  void flushPostLikeCountRestoresDeltaWhenDatabaseWriteFails() {
    when(postLikeRepository.drainDelta(10L)).thenReturn(3L);
    doThrow(new RuntimeException("db failure"))
        .when(postMapper).applyLikeCountDelta(10L, 3L);

    try {
      postLikeCountFlushService.flushPostLikeCount(10L);
    } catch (RuntimeException ignored) {
      // expected
    }

    verify(postLikeRepository).addDelta(10L, 3L);
  }
}

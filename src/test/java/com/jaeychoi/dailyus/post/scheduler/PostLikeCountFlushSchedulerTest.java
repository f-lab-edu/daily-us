package com.jaeychoi.dailyus.post.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.post.repository.PostLikeRepository;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostLikeCountFlushSchedulerTest {

  @Mock
  private PostLikeRepository postLikeRepository;

  @Mock
  private PostMapper postMapper;

  @InjectMocks
  private PostLikeCountFlushScheduler postLikeCountFlushScheduler;

  @Test
  void flushAppliesDrainedDeltasToPosts() throws Exception {
    setFlushBatchSize(2L);
    when(postLikeRepository.popDirtyPostIds(2L)).thenReturn(List.of(10L));
    when(postLikeRepository.drainDelta(10L)).thenReturn(3L);

    postLikeCountFlushScheduler.flush();

    verify(postMapper).applyLikeCountDelta(10L, 3L);
  }

  @Test
  void flushSkipsWhenDrainedDeltaIsZero() throws Exception {
    setFlushBatchSize(2L);
    when(postLikeRepository.popDirtyPostIds(2L)).thenReturn(List.of(10L));
    when(postLikeRepository.drainDelta(10L)).thenReturn(0L);

    postLikeCountFlushScheduler.flush();

    verify(postMapper, never()).applyLikeCountDelta(10L, 0L);
  }

  @Test
  void flushRestoresDeltaWhenDatabaseWriteFails() throws Exception {
    setFlushBatchSize(2L);
    when(postLikeRepository.popDirtyPostIds(2L)).thenReturn(List.of(10L));
    when(postLikeRepository.drainDelta(10L)).thenReturn(3L);
    doThrow(new RuntimeException("db failure"))
        .when(postMapper).applyLikeCountDelta(10L, 3L);

    try {
      postLikeCountFlushScheduler.flush();
    } catch (RuntimeException ignored) {
      // expected
    }

    verify(postLikeRepository).addDelta(10L, 3L);
  }

  private void setFlushBatchSize(long flushBatchSize) throws Exception {
    Field field = PostLikeCountFlushScheduler.class.getDeclaredField("flushBatchSize");
    field.setAccessible(true);
    field.set(postLikeCountFlushScheduler, flushBatchSize);
  }
}

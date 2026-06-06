package com.jaeychoi.dailyus.post.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.post.repository.PostLikeRepository;
import com.jaeychoi.dailyus.post.service.PostLikeCountFlushService;
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
  void flushRunsConfiguredBatch() throws Exception {
    setFlushBatchSize(2L);
    when(postLikeRepository.popDirtyPostIds(2L)).thenReturn(List.of(10L, 20L));
    when(postLikeRepository.drainDelta(10L)).thenReturn(3L);
    when(postLikeRepository.drainDelta(20L)).thenReturn(1L);

    postLikeCountFlushScheduler.flush();

    verify(postLikeRepository).popDirtyPostIds(2L);
    verify(postLikeRepository).drainDelta(10L);
    verify(postLikeRepository).drainDelta(20L);
    verify(postMapper).applyLikeCountDelta(10L, 3L);
    verify(postMapper).applyLikeCountDelta(20L, 1L);
  }

  @Test
  void flushSkipsPostUpdateWhenDrainedDeltaIsZero() throws Exception {
    setFlushBatchSize(2L);
    when(postLikeRepository.popDirtyPostIds(2L)).thenReturn(List.of(10L));
    when(postLikeRepository.drainDelta(10L)).thenReturn(0L);

    postLikeCountFlushScheduler.flush();

    verify(postLikeRepository).popDirtyPostIds(2L);
    verify(postLikeRepository).drainDelta(10L);
    verify(postMapper, never()).applyLikeCountDelta(10L, 0L);
  }

  @Test
  void flushRestoresDeltaWhenPostUpdateFails() throws Exception {
    setFlushBatchSize(2L);
    when(postLikeRepository.popDirtyPostIds(2L)).thenReturn(List.of(10L));
    when(postLikeRepository.drainDelta(10L)).thenReturn(2L);
    org.mockito.Mockito.doThrow(new RuntimeException("flush failed"))
        .when(postMapper)
        .applyLikeCountDelta(10L, 2L);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> postLikeCountFlushScheduler.flush())
        .isInstanceOf(RuntimeException.class)
        .hasMessage("flush failed");

    verify(postLikeRepository).addDelta(10L, 2L);
    verify(postMapper, times(1)).applyLikeCountDelta(10L, 2L);
  }

  private void setFlushBatchSize(long flushBatchSize) throws Exception {
    Field field = PostLikeCountFlushScheduler.class.getDeclaredField("flushBatchSize");
    field.setAccessible(true);
    field.set(postLikeCountFlushScheduler, flushBatchSize);
  }
}

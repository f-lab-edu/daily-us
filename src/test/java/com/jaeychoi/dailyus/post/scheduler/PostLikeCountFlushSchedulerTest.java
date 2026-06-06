package com.jaeychoi.dailyus.post.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.post.service.PostLikeCountFlushService;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostLikeCountFlushSchedulerTest {

  @Mock
  private PostLikeCountFlushService postLikeCountFlushService;

  @InjectMocks
  private PostLikeCountFlushScheduler postLikeCountFlushScheduler;

  @Test
  void flushRunsConfiguredBatch() throws Exception {
    setFlushBatchSize(2L);
    when(postLikeCountFlushService.flushBatch(2L)).thenReturn(1);

    postLikeCountFlushScheduler.flush();

    verify(postLikeCountFlushService).flushBatch(2L);
  }

  @Test
  void flushStillDelegatesWhenNothingWasFlushed() throws Exception {
    setFlushBatchSize(2L);
    when(postLikeCountFlushService.flushBatch(2L)).thenReturn(0);

    postLikeCountFlushScheduler.flush();

    verify(postLikeCountFlushService).flushBatch(2L);
    verify(postLikeCountFlushService, never()).flushAllDirty();
  }

  private void setFlushBatchSize(long flushBatchSize) throws Exception {
    Field field = PostLikeCountFlushScheduler.class.getDeclaredField("flushBatchSize");
    field.setAccessible(true);
    field.set(postLikeCountFlushScheduler, flushBatchSize);
  }
}

package com.jaeychoi.dailyus.post.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.jaeychoi.dailyus.common.app.FeedCacheWarmupProperties;
import com.jaeychoi.dailyus.post.service.PostFeedCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostFeedCacheStartupRunnerTest {

  @Mock
  private PostFeedCacheService postFeedCacheService;

  @Test
  void refreshFeedCachesOnStartupSkipsWhenDisabled() {
    PostFeedCacheStartupRunner runner = new PostFeedCacheStartupRunner(
        postFeedCacheService,
        new FeedCacheWarmupProperties(false)
    );

    runner.refreshFeedCachesOnStartup();

    verify(postFeedCacheService, never()).refreshUserFeedCache(1L);
  }

  @Test
  void refreshFeedCachesOnStartupRefreshesUsersFrom1To100() {
    PostFeedCacheStartupRunner runner = new PostFeedCacheStartupRunner(
        postFeedCacheService,
        new FeedCacheWarmupProperties(true)
    );

    runner.refreshFeedCachesOnStartup();

    verify(postFeedCacheService).refreshUserFeedCache(1L);
    verify(postFeedCacheService).refreshUserFeedCache(100L);
    verify(postFeedCacheService, times(100)).refreshUserFeedCache(org.mockito.ArgumentMatchers.anyLong());
  }
}

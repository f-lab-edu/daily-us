package com.jaeychoi.dailyus.post.scheduler;

import com.jaeychoi.dailyus.common.app.FeedCacheWarmupProperties;
import com.jaeychoi.dailyus.post.service.PostFeedCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostFeedCacheStartupRunner {

  private static final long START_USER_ID = 1L;
  private static final long END_USER_ID = 100L;

  private final PostFeedCacheService postFeedCacheService;
  private final FeedCacheWarmupProperties feedCacheWarmupProperties;

  @EventListener(ApplicationReadyEvent.class)
  public void refreshFeedCachesOnStartup() {
    if (!feedCacheWarmupProperties.enabled()) {
      log.info("feed cache startup refresh skipped. enabled=false");
      return;
    }

    int successCount = 0;
    int failureCount = 0;
    for (long userId = START_USER_ID; userId <= END_USER_ID; userId++) {
      try {
        postFeedCacheService.refreshUserFeedCache(userId);
        successCount++;
      } catch (RuntimeException e) {
        failureCount++;
        log.warn("feed cache startup refresh failed. userId={}", userId, e);
      }
    }

    log.info(
        "feed cache startup refresh finished. targetCount={}, successCount={}, failureCount={}",
        END_USER_ID - START_USER_ID + 1,
        successCount,
        failureCount
    );
  }
}

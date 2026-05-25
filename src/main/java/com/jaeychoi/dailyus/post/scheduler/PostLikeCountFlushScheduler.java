package com.jaeychoi.dailyus.post.scheduler;

import com.jaeychoi.dailyus.post.service.PostLikeCountFlushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostLikeCountFlushScheduler {

  private final PostLikeCountFlushService postLikeCountFlushService;

  @Value("${app.like.flush-batch-size:100}")
  private long flushBatchSize;

  @Scheduled(fixedDelayString = "${app.like.flush-delay-ms:1000}")
  public void flush() {
    int flushedCount = postLikeCountFlushService.flushBatch(flushBatchSize);
    log.debug("post like count flush finished. flushedCount={}", flushedCount);
  }
}

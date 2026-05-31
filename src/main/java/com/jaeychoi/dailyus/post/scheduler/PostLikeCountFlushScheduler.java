package com.jaeychoi.dailyus.post.scheduler;

import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.post.repository.PostLikeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostLikeCountFlushScheduler {

  private static final Logger log = LoggerFactory.getLogger(PostLikeCountFlushScheduler.class);
  private final PostLikeRepository postLikeRepository;
  private final PostMapper postMapper;

  @Value("${app.like.flush-batch-size:100}")
  private long flushBatchSize;

  @Scheduled(fixedDelayString = "${app.like.flush-delay-ms:1000}")
  public void flush() {
    List<Long> postIds = postLikeRepository.popDirtyPostIds(flushBatchSize);
    for (Long postId : postIds) {
      flushPostLikeCount(postId);
    }
  }

  private void flushPostLikeCount(Long postId) {
    long delta = postLikeRepository.drainDelta(postId);
    if (delta == 0L) {
      return;
    }

    try {
      postMapper.applyLikeCountDelta(postId, delta);
    } catch (RuntimeException e) {
      log.error("Failed to flush post like count. postId={}, delta={}", postId, delta, e);
      postLikeRepository.addDelta(postId, delta);
      throw e;
    }
  }
}

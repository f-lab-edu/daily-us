package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.post.repository.PostLikeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeCountFlushService {

  private final PostLikeRepository postLikeRepository;
  private final PostMapper postMapper;

  public int flushBatch(long batchSize) {
    List<Long> postIds = postLikeRepository.popDirtyPostIds(batchSize);
    int flushedCount = 0;
    for (Long postId : postIds) {
      flushPostLikeCount(postId);
      flushedCount++;
    }
    return flushedCount;
  }

  public int flushAllDirty() {
    int totalFlushedCount = 0;
    while (true) {
      int flushedCount = flushBatch(Long.MAX_VALUE);
      if (flushedCount == 0) {
        return totalFlushedCount;
      }
      totalFlushedCount += flushedCount;
    }
  }

  public void flushPostLikeCount(Long postId) {
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

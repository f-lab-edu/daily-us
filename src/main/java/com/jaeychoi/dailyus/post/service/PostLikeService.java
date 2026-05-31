package com.jaeychoi.dailyus.post.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.dto.PostLikeResponse;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.post.repository.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService {

  private final PostMapper postMapper;
  private final PostLikeRepository postLikeRepository;

  @Transactional
  public PostLikeResponse like(Long userId, Long postId) {
    validatePostExists(postId);

    try {
      postMapper.insertLike(postId, userId);
    } catch (DuplicateKeyException e) {
      throw new BaseException(ErrorCode.POST_LIKE_ALREADY_EXISTS);
    }

    scheduleAfterCommit(() -> postLikeRepository.incrementDelta(postId));

    return new PostLikeResponse(postId, true, getLikeCount(postId, 1L));
  }

  @Transactional
  public PostLikeResponse unlike(Long userId, Long postId) {
    validatePostExists(postId);

    int deletedCount = postMapper.deleteLike(postId, userId);
    if (deletedCount == 0) {
      throw new BaseException(ErrorCode.POST_LIKE_NOT_FOUND);
    }

    scheduleAfterCommit(() -> postLikeRepository.decrementDelta(postId));

    return new PostLikeResponse(postId, false, getLikeCount(postId, -1L));
  }

  private void validatePostExists(Long postId) {
    if (!postMapper.existsActiveById(postId)) {
      throw new BaseException(ErrorCode.POST_NOT_FOUND);
    }
  }

  private Long getLikeCount(Long postId, long delta) {
    return Math.max(postMapper.findLikeCountByPostId(postId) + delta, 0L);
  }

  private void scheduleAfterCommit(Runnable action) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      action.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        action.run();
      }
    });
  }
}

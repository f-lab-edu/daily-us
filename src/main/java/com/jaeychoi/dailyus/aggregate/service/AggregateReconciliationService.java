package com.jaeychoi.dailyus.aggregate.service;

import com.jaeychoi.dailyus.aggregate.dto.AggregateReconciliationResult;
import com.jaeychoi.dailyus.aggregate.mapper.AggregateReconciliationMapper;
import com.jaeychoi.dailyus.post.service.PostLikeCountFlushService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AggregateReconciliationService {

  private static final String POST_LIKE_TARGET = "posts.like_count";
  private static final String COMMENT_LIKE_TARGET = "comments.like_count";
  private static final String FOLLOWER_TARGET = "users.follower_count";
  private static final String FOLLOWEE_TARGET = "users.followee_count";
  private static final String MEMBER_TARGET = "user_groups.member_count";

  private final AggregateReconciliationMapper aggregateReconciliationMapper;
  private final PostLikeCountFlushService postLikeCountFlushService;

  @Transactional
  public AggregateReconciliationResult reconcilePostLikeCounts(int batchSize) {
    validateBatchSize(batchSize);
    postLikeCountFlushService.flushAllDirty();
    return reconcile(
        POST_LIKE_TARGET,
        batchSize,
        aggregateReconciliationMapper::findPostIdsWithLikeCountMismatch,
        aggregateReconciliationMapper::reconcilePostLikeCount
    );
  }

  @Transactional
  public AggregateReconciliationResult reconcileCommentLikeCounts(int batchSize) {
    return reconcile(
        COMMENT_LIKE_TARGET,
        batchSize,
        aggregateReconciliationMapper::findCommentIdsWithLikeCountMismatch,
        aggregateReconciliationMapper::reconcileCommentLikeCount
    );
  }

  @Transactional
  public AggregateReconciliationResult reconcileFollowerCounts(int batchSize) {
    return reconcile(
        FOLLOWER_TARGET,
        batchSize,
        aggregateReconciliationMapper::findUserIdsWithFollowerCountMismatch,
        aggregateReconciliationMapper::reconcileFollowerCount
    );
  }

  @Transactional
  public AggregateReconciliationResult reconcileFolloweeCounts(int batchSize) {
    return reconcile(
        FOLLOWEE_TARGET,
        batchSize,
        aggregateReconciliationMapper::findUserIdsWithFolloweeCountMismatch,
        aggregateReconciliationMapper::reconcileFolloweeCount
    );
  }

  @Transactional
  public AggregateReconciliationResult reconcileMemberCounts(int batchSize) {
    return reconcile(
        MEMBER_TARGET,
        batchSize,
        aggregateReconciliationMapper::findGroupIdsWithMemberCountMismatch,
        aggregateReconciliationMapper::reconcileMemberCount
    );
  }

  private AggregateReconciliationResult reconcile(
      String target,
      int batchSize,
      IdLookup idLookup,
      Updater updater
  ) {
    validateBatchSize(batchSize);

    int batchCount = 0;
    int updatedCount = 0;

    while (true) {
      List<Long> ids = idLookup.findMismatchIds(batchSize);
      if (ids.isEmpty()) {
        return new AggregateReconciliationResult(target, batchCount, updatedCount);
      }

      batchCount++;
      for (Long id : ids) {
        updatedCount += updater.update(id);
      }
    }
  }

  private void validateBatchSize(int batchSize) {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("batchSize must be greater than 0");
    }
  }

  @FunctionalInterface
  private interface IdLookup {

    List<Long> findMismatchIds(int limit);
  }

  @FunctionalInterface
  private interface Updater {

    int update(Long id);
  }
}

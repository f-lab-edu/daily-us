package com.jaeychoi.dailyus.aggregate.service;

import com.jaeychoi.dailyus.aggregate.dto.AggregateReconciliationResult;
import com.jaeychoi.dailyus.aggregate.internal.AggregateReconciliationTxExecutor;
import com.jaeychoi.dailyus.post.service.PostLikeCountFlushService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AggregateReconciliationService {

  private static final String POST_LIKE_TARGET = "posts.like_count";
  private static final String COMMENT_LIKE_TARGET = "comments.like_count";
  private static final String FOLLOWER_TARGET = "users.follower_count";
  private static final String FOLLOWEE_TARGET = "users.followee_count";
  private static final String MEMBER_TARGET = "user_groups.member_count";

  private final PostLikeCountFlushService postLikeCountFlushService;
  private final AggregateReconciliationTxExecutor aggregateReconciliationTxExecutor;

  public AggregateReconciliationResult reconcilePostLikeCounts(int batchSize) {
    validateBatchSize(batchSize);
    postLikeCountFlushService.flushAllDirty();
    return reconcile(
        POST_LIKE_TARGET,
        batchSize,
        aggregateReconciliationTxExecutor::reconcilePostLikeBatch
    );
  }

  public AggregateReconciliationResult reconcileCommentLikeCounts(int batchSize) {
    return reconcile(
        COMMENT_LIKE_TARGET,
        batchSize,
        aggregateReconciliationTxExecutor::reconcileCommentLikeBatch
    );
  }

  public AggregateReconciliationResult reconcileFollowerCounts(int batchSize) {
    return reconcile(
        FOLLOWER_TARGET,
        batchSize,
        aggregateReconciliationTxExecutor::reconcileFollowerBatch
    );
  }

  public AggregateReconciliationResult reconcileFolloweeCounts(int batchSize) {
    return reconcile(
        FOLLOWEE_TARGET,
        batchSize,
        aggregateReconciliationTxExecutor::reconcileFolloweeBatch
    );
  }

  public AggregateReconciliationResult reconcileMemberCounts(int batchSize) {
    return reconcile(
        MEMBER_TARGET,
        batchSize,
        aggregateReconciliationTxExecutor::reconcileMemberBatch
    );
  }

  private AggregateReconciliationResult reconcile(
      String target,
      int batchSize,
      BatchRunner batchRunner
  ) {
    validateBatchSize(batchSize);

    int batchCount = 0;
    int updatedCount = 0;

    while (true) {
      AggregateReconciliationTxExecutor.BatchReconciliationResult batchResult =
          batchRunner.run(batchSize);
      if (batchResult.completed()) {
        return new AggregateReconciliationResult(target, batchCount, updatedCount);
      }

      batchCount++;
      updatedCount += batchResult.updatedCount();
    }
  }

  private void validateBatchSize(int batchSize) {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("batchSize must be greater than 0");
    }
  }

  @FunctionalInterface
  private interface BatchRunner {

    AggregateReconciliationTxExecutor.BatchReconciliationResult run(int batchSize);
  }
}

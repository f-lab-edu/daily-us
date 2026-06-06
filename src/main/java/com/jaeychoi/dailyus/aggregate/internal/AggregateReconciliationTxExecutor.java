package com.jaeychoi.dailyus.aggregate.internal;

import com.jaeychoi.dailyus.aggregate.mapper.AggregateReconciliationMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AggregateReconciliationTxExecutor {

  private final AggregateReconciliationMapper aggregateReconciliationMapper;

  @Transactional
  public BatchReconciliationResult reconcilePostLikeBatch(int batchSize) {
    return reconcileBatch(
        batchSize,
        aggregateReconciliationMapper::findPostIdsWithLikeCountMismatch,
        aggregateReconciliationMapper::reconcilePostLikeCount
    );
  }

  @Transactional
  public BatchReconciliationResult reconcileCommentLikeBatch(int batchSize) {
    return reconcileBatch(
        batchSize,
        aggregateReconciliationMapper::findCommentIdsWithLikeCountMismatch,
        aggregateReconciliationMapper::reconcileCommentLikeCount
    );
  }

  @Transactional
  public BatchReconciliationResult reconcileFollowerBatch(int batchSize) {
    return reconcileBatch(
        batchSize,
        aggregateReconciliationMapper::findUserIdsWithFollowerCountMismatch,
        aggregateReconciliationMapper::reconcileFollowerCount
    );
  }

  @Transactional
  public BatchReconciliationResult reconcileFolloweeBatch(int batchSize) {
    return reconcileBatch(
        batchSize,
        aggregateReconciliationMapper::findUserIdsWithFolloweeCountMismatch,
        aggregateReconciliationMapper::reconcileFolloweeCount
    );
  }

  @Transactional
  public BatchReconciliationResult reconcileMemberBatch(int batchSize) {
    return reconcileBatch(
        batchSize,
        aggregateReconciliationMapper::findGroupIdsWithMemberCountMismatch,
        aggregateReconciliationMapper::reconcileMemberCount
    );
  }

  private BatchReconciliationResult reconcileBatch(
      int batchSize,
      IdLookup idLookup,
      Updater updater
  ) {
    List<Long> ids = idLookup.findMismatchIds(batchSize);
    if (ids.isEmpty()) {
      return new BatchReconciliationResult(true, 0);
    }

    int updatedCount = 0;
    for (Long id : ids) {
      updatedCount += updater.update(id);
    }

    return new BatchReconciliationResult(false, updatedCount);
  }

  public record BatchReconciliationResult(boolean completed, int updatedCount) {
  }

  @FunctionalInterface
  public interface IdLookup {

    List<Long> findMismatchIds(int limit);
  }

  @FunctionalInterface
  public interface Updater {

    int update(Long id);
  }
}

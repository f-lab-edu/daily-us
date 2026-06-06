package com.jaeychoi.dailyus.aggregate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.aggregate.dto.AggregateReconciliationResult;
import com.jaeychoi.dailyus.aggregate.internal.AggregateReconciliationTxExecutor;
import com.jaeychoi.dailyus.post.service.PostLikeCountFlushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AggregateReconciliationServiceTest {

  @Mock
  private PostLikeCountFlushService postLikeCountFlushService;

  @Mock
  private AggregateReconciliationTxExecutor aggregateReconciliationTxExecutor;

  @InjectMocks
  private AggregateReconciliationService aggregateReconciliationService;

  @Test
  void reconcilePostLikeCountsFlushesPendingDeltasBeforeReconciling() {
    when(aggregateReconciliationTxExecutor.reconcilePostLikeBatch(2))
        .thenReturn(new AggregateReconciliationTxExecutor.BatchReconciliationResult(false, 2))
        .thenReturn(new AggregateReconciliationTxExecutor.BatchReconciliationResult(true, 0));

    AggregateReconciliationResult result =
        aggregateReconciliationService.reconcilePostLikeCounts(2);

    verify(postLikeCountFlushService).flushAllDirty();
    assertThat(result).isEqualTo(new AggregateReconciliationResult("posts.like_count", 1, 2));
  }

  @Test
  void reconcileCommentLikeCountsReturnsZeroWhenNoMismatchExists() {
    when(aggregateReconciliationTxExecutor.reconcileCommentLikeBatch(100))
        .thenReturn(new AggregateReconciliationTxExecutor.BatchReconciliationResult(true, 0));

    AggregateReconciliationResult result =
        aggregateReconciliationService.reconcileCommentLikeCounts(100);

    assertThat(result).isEqualTo(new AggregateReconciliationResult("comments.like_count", 0, 0));
  }

  @Test
  void reconcileFollowerCountsAggregatesMultipleTransactionalBatches() {
    when(aggregateReconciliationTxExecutor.reconcileFollowerBatch(100))
        .thenReturn(new AggregateReconciliationTxExecutor.BatchReconciliationResult(false, 2))
        .thenReturn(new AggregateReconciliationTxExecutor.BatchReconciliationResult(false, 1))
        .thenReturn(new AggregateReconciliationTxExecutor.BatchReconciliationResult(true, 0));

    AggregateReconciliationResult result =
        aggregateReconciliationService.reconcileFollowerCounts(100);

    assertThat(result).isEqualTo(new AggregateReconciliationResult("users.follower_count", 2, 3));
  }

  @Test
  void reconcileThrowsWhenBatchSizeIsNotPositive() {
    assertThatThrownBy(() -> aggregateReconciliationService.reconcilePostLikeCounts(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("batchSize must be greater than 0");
  }
}

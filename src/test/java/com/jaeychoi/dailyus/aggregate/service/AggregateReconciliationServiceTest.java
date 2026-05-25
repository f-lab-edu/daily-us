package com.jaeychoi.dailyus.aggregate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.aggregate.dto.AggregateReconciliationResult;
import com.jaeychoi.dailyus.aggregate.mapper.AggregateReconciliationMapper;
import com.jaeychoi.dailyus.post.service.PostLikeCountFlushService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AggregateReconciliationServiceTest {

  @Mock
  private AggregateReconciliationMapper aggregateReconciliationMapper;

  @Mock
  private PostLikeCountFlushService postLikeCountFlushService;

  @InjectMocks
  private AggregateReconciliationService aggregateReconciliationService;

  @Test
  void reconcilePostLikeCountsFlushesPendingDeltasBeforeReconciling() {
    when(aggregateReconciliationMapper.findPostIdsWithLikeCountMismatch(2))
        .thenReturn(List.of(10L, 20L))
        .thenReturn(List.of());
    when(aggregateReconciliationMapper.reconcilePostLikeCount(10L)).thenReturn(1);
    when(aggregateReconciliationMapper.reconcilePostLikeCount(20L)).thenReturn(1);

    AggregateReconciliationResult result =
        aggregateReconciliationService.reconcilePostLikeCounts(2);

    verify(postLikeCountFlushService).flushAllDirty();
    assertThat(result).isEqualTo(new AggregateReconciliationResult("posts.like_count", 1, 2));
  }

  @Test
  void reconcileCommentLikeCountsReturnsZeroWhenNoMismatchExists() {
    when(aggregateReconciliationMapper.findCommentIdsWithLikeCountMismatch(100))
        .thenReturn(List.of());

    AggregateReconciliationResult result =
        aggregateReconciliationService.reconcileCommentLikeCounts(100);

    assertThat(result).isEqualTo(new AggregateReconciliationResult("comments.like_count", 0, 0));
  }

  @Test
  void reconcileThrowsWhenBatchSizeIsNotPositive() {
    assertThatThrownBy(() -> aggregateReconciliationService.reconcilePostLikeCounts(0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("batchSize must be greater than 0");
  }
}

package com.jaeychoi.dailyus.aggregate.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.aggregate.mapper.AggregateReconciliationMapper;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class AggregateReconciliationTxExecutorTest {

  @Mock
  private AggregateReconciliationMapper aggregateReconciliationMapper;

  @InjectMocks
  private AggregateReconciliationTxExecutor aggregateReconciliationTxExecutor;

  @Test
  void publicBatchEntryPointsCarryTransactionalBoundary() throws Exception {
    assertThat(isTransactional("reconcilePostLikeBatch")).isTrue();
    assertThat(isTransactional("reconcileCommentLikeBatch")).isTrue();
    assertThat(isTransactional("reconcileFollowerBatch")).isTrue();
    assertThat(isTransactional("reconcileFolloweeBatch")).isTrue();
    assertThat(isTransactional("reconcileMemberBatch")).isTrue();
  }

  @Test
  void reconcilePostLikeBatchReturnsCompletedWhenNoMismatchExists() {
    when(aggregateReconciliationMapper.findPostIdsWithLikeCountMismatch(50))
        .thenReturn(List.of());

    AggregateReconciliationTxExecutor.BatchReconciliationResult result =
        aggregateReconciliationTxExecutor.reconcilePostLikeBatch(50);

    assertThat(result).isEqualTo(
        new AggregateReconciliationTxExecutor.BatchReconciliationResult(true, 0)
    );
    verify(aggregateReconciliationMapper).findPostIdsWithLikeCountMismatch(50);
    verifyNoMoreInteractions(aggregateReconciliationMapper);
  }

  @Test
  void reconcileFollowerBatchUpdatesEveryReturnedIdWithinOneBatch() {
    when(aggregateReconciliationMapper.findUserIdsWithFollowerCountMismatch(100))
        .thenReturn(List.of(10L, 20L));
    when(aggregateReconciliationMapper.reconcileFollowerCount(10L)).thenReturn(1);
    when(aggregateReconciliationMapper.reconcileFollowerCount(20L)).thenReturn(1);

    AggregateReconciliationTxExecutor.BatchReconciliationResult result =
        aggregateReconciliationTxExecutor.reconcileFollowerBatch(100);

    assertThat(result).isEqualTo(
        new AggregateReconciliationTxExecutor.BatchReconciliationResult(false, 2)
    );
    verify(aggregateReconciliationMapper).reconcileFollowerCount(10L);
    verify(aggregateReconciliationMapper).reconcileFollowerCount(20L);
  }

  private boolean isTransactional(String methodName) throws Exception {
    Method method = AggregateReconciliationTxExecutor.class.getDeclaredMethod(methodName, int.class);
    return method.isAnnotationPresent(Transactional.class);
  }
}

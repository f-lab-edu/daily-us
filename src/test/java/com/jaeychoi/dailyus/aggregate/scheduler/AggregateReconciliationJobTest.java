package com.jaeychoi.dailyus.aggregate.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.jaeychoi.dailyus.aggregate.dto.AggregateReconciliationResult;
import com.jaeychoi.dailyus.aggregate.service.AggregateReconciliationService;
import com.jaeychoi.dailyus.common.app.AggregateReconciliationProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AggregateReconciliationJobTest {

  @Mock
  private AggregateReconciliationService aggregateReconciliationService;

  @Test
  void reconcileHotAggregatesSkipsWhenDisabled() {
    AggregateReconciliationJob job = new AggregateReconciliationJob(
        aggregateReconciliationService,
        new AggregateReconciliationProperties(
            new AggregateReconciliationProperties.JobProperties(false, "0 */5 * * * *", 200),
            new AggregateReconciliationProperties.JobProperties(false, "0 0 4 * * *", 1000)
        )
    );

    job.reconcileHotAggregates();

    verify(aggregateReconciliationService, never()).reconcilePostLikeCounts(200);
    verify(aggregateReconciliationService, never()).reconcileCommentLikeCounts(200);
  }

  @Test
  void reconcileHotAggregatesRunsOnlyHotAggregateJobsWithConfiguredBatchSize() {
    AggregateReconciliationJob job = new AggregateReconciliationJob(
        aggregateReconciliationService,
        new AggregateReconciliationProperties(
            new AggregateReconciliationProperties.JobProperties(true, "0 */5 * * * *", 250),
            new AggregateReconciliationProperties.JobProperties(false, "0 0 4 * * *", 1000)
        )
    );
    org.mockito.Mockito.when(aggregateReconciliationService.reconcilePostLikeCounts(250))
        .thenReturn(new AggregateReconciliationResult("posts.like_count", 1, 1));
    org.mockito.Mockito.when(aggregateReconciliationService.reconcileCommentLikeCounts(250))
        .thenReturn(new AggregateReconciliationResult("comments.like_count", 1, 1));

    job.reconcileHotAggregates();

    verify(aggregateReconciliationService).reconcilePostLikeCounts(250);
    verify(aggregateReconciliationService).reconcileCommentLikeCounts(250);
    verify(aggregateReconciliationService, never()).reconcileFollowerCounts(250);
    verify(aggregateReconciliationService, never()).reconcileFolloweeCounts(250);
    verify(aggregateReconciliationService, never()).reconcileMemberCounts(250);
  }

  @Test
  void reconcileFullAggregatesRunsAllAggregateJobsWithConfiguredBatchSize() {
    AggregateReconciliationJob job = new AggregateReconciliationJob(
        aggregateReconciliationService,
        new AggregateReconciliationProperties(
            new AggregateReconciliationProperties.JobProperties(false, "0 */5 * * * *", 250),
            new AggregateReconciliationProperties.JobProperties(true, "0 0 4 * * *", 1000)
        )
    );
    org.mockito.Mockito.when(aggregateReconciliationService.reconcilePostLikeCounts(1000))
        .thenReturn(new AggregateReconciliationResult("posts.like_count", 1, 1));
    org.mockito.Mockito.when(aggregateReconciliationService.reconcileCommentLikeCounts(1000))
        .thenReturn(new AggregateReconciliationResult("comments.like_count", 1, 1));
    org.mockito.Mockito.when(aggregateReconciliationService.reconcileFollowerCounts(1000))
        .thenReturn(new AggregateReconciliationResult("users.follower_count", 1, 1));
    org.mockito.Mockito.when(aggregateReconciliationService.reconcileFolloweeCounts(1000))
        .thenReturn(new AggregateReconciliationResult("users.followee_count", 1, 1));
    org.mockito.Mockito.when(aggregateReconciliationService.reconcileMemberCounts(1000))
        .thenReturn(new AggregateReconciliationResult("user_groups.member_count", 1, 1));

    job.reconcileFullAggregates();

    verify(aggregateReconciliationService).reconcilePostLikeCounts(1000);
    verify(aggregateReconciliationService).reconcileCommentLikeCounts(1000);
    verify(aggregateReconciliationService).reconcileFollowerCounts(1000);
    verify(aggregateReconciliationService).reconcileFolloweeCounts(1000);
    verify(aggregateReconciliationService).reconcileMemberCounts(1000);
  }
}

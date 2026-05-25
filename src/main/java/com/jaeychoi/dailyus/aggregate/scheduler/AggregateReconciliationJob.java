package com.jaeychoi.dailyus.aggregate.scheduler;

import com.jaeychoi.dailyus.aggregate.dto.AggregateReconciliationResult;
import com.jaeychoi.dailyus.aggregate.service.AggregateReconciliationService;
import com.jaeychoi.dailyus.common.app.AggregateReconciliationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregateReconciliationJob {

  private final AggregateReconciliationService aggregateReconciliationService;
  private final AggregateReconciliationProperties aggregateReconciliationProperties;

  @Scheduled(cron = "${app.aggregate-reconciliation.hot.cron}")
  public void reconcileHotAggregates() {
    AggregateReconciliationProperties.JobProperties hot = aggregateReconciliationProperties.hot();
    if (!hot.enabled()) {
      log.info("aggregate reconciliation skipped. scope=hot, enabled=false");
      return;
    }

    int batchSize = hot.batchSize();
    logResult("hot", aggregateReconciliationService.reconcilePostLikeCounts(batchSize));
    logResult("hot", aggregateReconciliationService.reconcileCommentLikeCounts(batchSize));
  }

  @Scheduled(cron = "${app.aggregate-reconciliation.full.cron}")
  public void reconcileFullAggregates() {
    AggregateReconciliationProperties.JobProperties full =
        aggregateReconciliationProperties.full();
    if (!full.enabled()) {
      log.info("aggregate reconciliation skipped. scope=full, enabled=false");
      return;
    }

    int batchSize = full.batchSize();
    logResult("full", aggregateReconciliationService.reconcilePostLikeCounts(batchSize));
    logResult("full", aggregateReconciliationService.reconcileCommentLikeCounts(batchSize));
    logResult("full", aggregateReconciliationService.reconcileFollowerCounts(batchSize));
    logResult("full", aggregateReconciliationService.reconcileFolloweeCounts(batchSize));
    logResult("full", aggregateReconciliationService.reconcileMemberCounts(batchSize));
  }

  private void logResult(String scope, AggregateReconciliationResult result) {
    log.info(
        "aggregate reconciliation finished. scope={}, target={}, batchCount={}, updatedCount={}",
        scope,
        result.target(),
        result.batchCount(),
        result.updatedCount()
    );
  }
}

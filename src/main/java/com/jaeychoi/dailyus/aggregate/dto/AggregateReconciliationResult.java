package com.jaeychoi.dailyus.aggregate.dto;

public record AggregateReconciliationResult(
    String target,
    int batchCount,
    int updatedCount
) {
}

package com.jaeychoi.dailyus.common.app;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.aggregate-reconciliation")
public record AggregateReconciliationProperties(
    JobProperties hot,
    JobProperties full
) {

  public record JobProperties(
      boolean enabled,
      String cron,
      int batchSize
  ) {
  }
}

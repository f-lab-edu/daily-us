package com.jaeychoi.dailyus.common.app;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.feed-cache.hybrid")
public record FeedCacheHybridProperties(
    boolean enabled,
    long hotAuthorThreshold
) {

  public FeedCacheHybridProperties {
    if (hotAuthorThreshold <= 0) {
      hotAuthorThreshold = Long.MAX_VALUE;
    }
  }
}

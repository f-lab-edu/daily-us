package com.jaeychoi.dailyus.common.app;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.feed-cache.warmup")
public record FeedCacheWarmupProperties(
    boolean enabled
) {
}

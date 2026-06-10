package com.jaeychoi.dailyus.common.config;

import com.jaeychoi.dailyus.common.app.FeedCacheHybridProperties;
import com.jaeychoi.dailyus.common.app.FeedCacheWarmupProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    FeedCacheWarmupProperties.class,
    FeedCacheHybridProperties.class
})
public class FeedCacheWarmupConfig {

}

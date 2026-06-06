package com.jaeychoi.dailyus.common.config;

import com.jaeychoi.dailyus.common.app.AggregateReconciliationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AggregateReconciliationProperties.class)
public class AggregateReconciliationConfig {

}

package com.jaeychoi.dailyus.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

  @Bean
  public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
    return new StringRedisTemplate(cf);
  }

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
    ObjectMapper objectMapper = new ObjectMapper();
    RedisTemplate<String, Object> t = new RedisTemplate<>();
    t.setConnectionFactory(connectionFactory);
    t.setKeySerializer(new StringRedisSerializer());
    t.setHashKeySerializer(new StringRedisSerializer());
    t.setValueSerializer(new GenericJacksonJsonRedisSerializer(objectMapper));
    t.setHashValueSerializer(new GenericJacksonJsonRedisSerializer(objectMapper));
    t.afterPropertiesSet();
    return t;
  }
}

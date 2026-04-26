package com.jaeychoi.dailyus.common.config;

import com.jaeychoi.dailyus.post.event.PostCreatedEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

  @Bean
  public ConsumerFactory<String, PostCreatedEvent> postCreatedConsumerFactory(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
  ) {
    JacksonJsonDeserializer<PostCreatedEvent> jsonDeserializer =
        new JacksonJsonDeserializer<>(PostCreatedEvent.class);
    jsonDeserializer.addTrustedPackages("*");
    jsonDeserializer.setUseTypeHeaders(false);

    Map<String, Object> properties = new HashMap<>();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return new DefaultKafkaConsumerFactory<>(properties, new StringDeserializer(),
        jsonDeserializer);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, PostCreatedEvent> postCreatedKafkaListenerContainerFactory(
      ConsumerFactory<String, PostCreatedEvent> postCreatedConsumerFactory
  ) {
    ConcurrentKafkaListenerContainerFactory<String, PostCreatedEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(postCreatedConsumerFactory);
    return factory;
  }
}

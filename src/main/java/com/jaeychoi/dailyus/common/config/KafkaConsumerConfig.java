package com.jaeychoi.dailyus.common.config;

import com.jaeychoi.dailyus.post.event.PostCreatedEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

  private static final long RETRY_INTERVAL_MS = 1_000L;
  private static final long RETRY_COUNT = 3L;

  @Bean
  public ConsumerFactory<String, PostCreatedEvent> postCreatedConsumerFactory(
      @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
  ) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    properties.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
    properties.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS,
        JacksonJsonDeserializer.class);
    properties.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, PostCreatedEvent.class.getName());
    properties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, "com.jaeychoi.dailyus.post.event");
    properties.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return new DefaultKafkaConsumerFactory<>(properties);
  }

  @Bean
  public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
        kafkaTemplate,
        (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition())
    );

    DefaultErrorHandler errorHandler =
        new DefaultErrorHandler(recoverer, new FixedBackOff(RETRY_INTERVAL_MS, RETRY_COUNT));
    errorHandler.addNotRetryableExceptions(
        DeserializationException.class,
        IllegalArgumentException.class
    );
    return errorHandler;
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, PostCreatedEvent> postCreatedKafkaListenerContainerFactory(
      ConsumerFactory<String, PostCreatedEvent> postCreatedConsumerFactory,
      DefaultErrorHandler kafkaErrorHandler
  ) {
    ConcurrentKafkaListenerContainerFactory<String, PostCreatedEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(postCreatedConsumerFactory);
    factory.setCommonErrorHandler(kafkaErrorHandler);
    return factory;
  }
}

package com.jaeychoi.dailyus.post.event;

import com.jaeychoi.dailyus.common.app.KafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostCreatedEventPublisher {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final KafkaTopicProperties topicProperties;

  public void publish(PostCreatedEvent event) {
    kafkaTemplate.send(topicProperties.postCreated(), String.valueOf(event.postId()), event);
  }
}

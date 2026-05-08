package com.jaeychoi.dailyus.post.event;

import com.jaeychoi.dailyus.post.service.PostFanoutService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostCreatedEventConsumer {

  private final PostFanoutService postFanoutService;

  @KafkaListener(
      topics = "${app.kafka.topics.post-created}",
      groupId = "post-feed-fanout-worker",
      containerFactory = "postCreatedKafkaListenerContainerFactory"
  )
  public void consume(PostCreatedEvent event) {
    postFanoutService.fanout(event);
  }
}

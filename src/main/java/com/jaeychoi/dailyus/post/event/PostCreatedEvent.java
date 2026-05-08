package com.jaeychoi.dailyus.post.event;

import java.time.LocalDateTime;

public record PostCreatedEvent(
    Long postId,
    Long userId,
    LocalDateTime createdAt
) {

}

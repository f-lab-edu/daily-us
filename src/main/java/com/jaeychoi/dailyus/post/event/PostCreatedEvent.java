package com.jaeychoi.dailyus.post.event;

public record PostCreatedEvent(
    Long postId,
    Long userId
) {

}

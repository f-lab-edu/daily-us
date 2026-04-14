package com.jaeychoi.dailyus.post.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostFeedResponse(
    List<PostFeedItemResponse> items,
    LocalDateTime lastCreatedAt,
    Long lastPostId,
    boolean hasNext,
    Long size) {

}

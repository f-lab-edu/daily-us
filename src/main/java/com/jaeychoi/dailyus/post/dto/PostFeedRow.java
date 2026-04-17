package com.jaeychoi.dailyus.post.dto;

import java.time.LocalDateTime;

public record PostFeedRow(
    Long postId,
    Long userId,
    String nickname,
    String profileImage,
    String content,
    Long likeCount,
    LocalDateTime createdAt
) {

}

package com.jaeychoi.dailyus.post.dto;

import java.time.LocalDateTime;

public record PostDetailRow(
    Long postId,
    Long userId,
    String nickname,
    String profileImage,
    String content,
    Long likeCount,
    Boolean likedByMe,
    LocalDateTime createdAt
) {

}

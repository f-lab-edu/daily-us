package com.jaeychoi.dailyus.post.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PostFeedItemResponse(
    Long postId,
    Long userId,
    String nickname,
    String profileImage,
    String content,
    List<String> imageUrls,
    Long likeCount,
    LocalDateTime createdAt
) {

}

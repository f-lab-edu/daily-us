package com.jaeychoi.dailyus.comment.dto;

import java.time.LocalDateTime;

public record CommentCreateResponse(
    Long commentId,
    Long postId,
    Long userId,
    String content,
    Long parentId,
    Long likeCount,
    LocalDateTime createdAt
) {
}

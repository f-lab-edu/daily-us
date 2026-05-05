package com.jaeychoi.dailyus.comment.dto;

import java.time.LocalDateTime;

public record CommentRow(
    Long commentId,
    Long userId,
    String nickname,
    String profileImage,
    String content,
    Long likeCount,
    Boolean likedByMe,
    LocalDateTime createdAt,
    Long parentId
) {
}

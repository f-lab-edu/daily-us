package com.jaeychoi.dailyus.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponseItem(
    Long commentId,
    Long userId,
    String nickname,
    String profileImage,
    String content,
    Long likeCount,
    Boolean likedByMe,
    LocalDateTime createdAt,
    Long parentId,
    List<CommentResponseItem> replies
) {
}

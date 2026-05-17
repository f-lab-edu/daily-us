package com.jaeychoi.dailyus.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
    List<CommentResponseItem> items,
    LocalDateTime lastCreatedAt,
    Long lastCommentId,
    boolean hasNext,
    Long size
) {
}

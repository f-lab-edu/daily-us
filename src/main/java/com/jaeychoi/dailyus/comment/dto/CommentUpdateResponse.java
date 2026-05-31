package com.jaeychoi.dailyus.comment.dto;

import java.time.LocalDateTime;

public record CommentUpdateResponse(
    Long commentId,
    String content,
    boolean edited
) {
}

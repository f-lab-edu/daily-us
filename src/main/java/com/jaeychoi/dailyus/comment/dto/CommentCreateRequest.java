package com.jaeychoi.dailyus.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
    @NotBlank
    @Size(max = 500)
    String content,
    Long parentId
) {
}

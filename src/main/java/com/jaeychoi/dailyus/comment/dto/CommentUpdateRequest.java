package com.jaeychoi.dailyus.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
    @NotBlank
    @Size(max = 500)
    String content
) {
}

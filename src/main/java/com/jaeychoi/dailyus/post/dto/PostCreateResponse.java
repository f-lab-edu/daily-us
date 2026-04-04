package com.jaeychoi.dailyus.post.dto;

import java.util.List;

public record PostCreateResponse(
    Long postId,
    Long userId,
    String content,
    List<String> imageUrls,
    List<String> hashtags,
    Long likeCount
) {

}

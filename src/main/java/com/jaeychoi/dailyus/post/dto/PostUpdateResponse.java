package com.jaeychoi.dailyus.post.dto;

import java.util.List;

public record PostUpdateResponse(
    Long postId,
    Long userId,
    String content,
    List<String> imageUrls,
    List<String> hashtags,
    Long likeCount
) {

}

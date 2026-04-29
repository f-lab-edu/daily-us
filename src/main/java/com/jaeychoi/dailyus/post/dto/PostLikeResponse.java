package com.jaeychoi.dailyus.post.dto;

public record PostLikeResponse(
    Long postId,
    boolean liked,
    Long likeCount
) {

}

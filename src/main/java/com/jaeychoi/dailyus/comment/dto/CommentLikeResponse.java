package com.jaeychoi.dailyus.comment.dto;

public record CommentLikeResponse(
    Long commentId,
    boolean liked,
    Long likeCount
) {

}

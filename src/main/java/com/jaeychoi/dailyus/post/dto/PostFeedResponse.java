package com.jaeychoi.dailyus.post.dto;

import java.util.List;

public record PostFeedResponse(
    List<PostFeedItemResponse> items,
    Long page,
    Long size) {

}

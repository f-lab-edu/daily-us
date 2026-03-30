package com.jaeychoi.dailyus.user.dto;

public record UserFollowResponse(
    Long followee,
    boolean following,
    Long followerCount,
    Long followeeCount
) {

}

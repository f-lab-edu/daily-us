package com.jaeychoi.dailyus.user.dto;

public record UserProfileResponse(
    Long userId,
    String email,
    String nickname,
    String intro,
    String profileImage,
    Long followerCount,
    Long followeeCount,
    Long postCount
) {
}

package com.jaeychoi.dailyus.group.dto;

public record GroupMemberResponse(
    Long userId,
    String nickname,
    String profileImage
) {
}

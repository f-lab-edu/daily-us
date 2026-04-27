package com.jaeychoi.dailyus.group.dto;

public record GroupMemberRankRow(
    Integer rank,
    Long userId,
    String nickname,
    String profileImage,
    Long postCount
) {

}

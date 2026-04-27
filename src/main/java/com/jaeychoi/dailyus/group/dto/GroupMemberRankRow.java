package com.jaeychoi.dailyus.group.dto;

public record GroupMemberRankRow(
    Integer ranking,
    Long userId,
    String nickname,
    String profileImage,
    Long postCount
) {

}

package com.jaeychoi.dailyus.group.dto;

public record GroupDetailResponse(
    Long groupId,
    String name,
    String intro,
    String groupImage,
    Long ownerId,
    String ownerNickname,
    Integer memberCount
) {
}

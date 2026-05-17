package com.jaeychoi.dailyus.group.dto;

public record GroupDetailRow(
    Long groupId,
    String name,
    String intro,
    String groupImage,
    Long ownerId,
    String ownerNickname,
    Integer memberCount
) {
}

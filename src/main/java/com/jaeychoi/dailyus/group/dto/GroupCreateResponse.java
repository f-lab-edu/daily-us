package com.jaeychoi.dailyus.group.dto;

public record GroupCreateResponse(
    Long groupId,
    String name,
    String intro,
    String groupImage,
    Long ownerId,
    Integer memberCount
) {

}

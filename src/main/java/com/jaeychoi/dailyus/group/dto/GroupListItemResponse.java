package com.jaeychoi.dailyus.group.dto;

import java.time.LocalDateTime;

public record GroupListItemResponse(
    Long groupId,
    String name,
    String groupImage,
    LocalDateTime createdAt
) {
}

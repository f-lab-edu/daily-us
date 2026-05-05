package com.jaeychoi.dailyus.group.dto;

import java.util.List;
import java.time.LocalDateTime;

public record GroupListResponse(
    List<GroupListItemResponse> items,
    LocalDateTime lastCreatedAt,
    Long lastGroupId,
    boolean hasNext,
    Long size
) {
}

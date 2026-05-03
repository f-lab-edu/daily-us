package com.jaeychoi.dailyus.user.dto;

import java.util.List;

public record UserActivityResponse(
    Integer year,
    Integer month,
    List<Integer> activityDays
) {
}

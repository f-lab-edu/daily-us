package com.jaeychoi.dailyus.group.dto;

import java.util.List;

public record GroupRankResponse(
    Long groupId,
    List<GroupMemberRankRow> rank
) {

}

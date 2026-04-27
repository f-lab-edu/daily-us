package com.jaeychoi.dailyus.group.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupMemberRankRow;
import com.jaeychoi.dailyus.group.dto.GroupRankResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupRankService {

  private final GroupMapper groupMapper;

  public GroupRankResponse getRank(Long groupId, Long userId) {
    Group group = groupMapper.findActiveById(groupId);
    if (group == null) {
      throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
    }
    validateMember(groupId, userId);

    LocalDateTime startAt = LocalDate.now().withDayOfMonth(1).atStartOfDay();
    LocalDateTime endAt = startAt.plusMonths(1);
    List<GroupMemberRankRow> rankRows = groupMapper.findMemberRanks(groupId, startAt, endAt);

    return new GroupRankResponse(groupId, rankRows);
  }

  private void validateMember(Long groupId, Long userId) {
    if (!groupMapper.existsMemberByIdAndMemberId(groupId, userId)) {
      throw new BaseException(ErrorCode.FORBIDDEN);
    }
  }
}

package com.jaeychoi.dailyus.group.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.dto.GroupDetailResponse;
import com.jaeychoi.dailyus.group.dto.GroupDetailRow;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupDetailService {

  private final GroupMapper groupMapper;

  public GroupDetailResponse getDetail(Long groupId) {
    GroupDetailRow row = groupMapper.findDetailById(groupId);
    if (row == null) {
      throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
    }

    return new GroupDetailResponse(
        row.groupId(),
        row.name(),
        row.intro(),
        row.groupImage(),
        row.ownerId(),
        row.ownerNickname(),
        row.memberCount()
    );
  }
}

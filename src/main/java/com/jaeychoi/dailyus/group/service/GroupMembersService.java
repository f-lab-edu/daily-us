package com.jaeychoi.dailyus.group.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.dto.GroupMemberResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupMembersService {

  private final GroupMapper groupMapper;

  public List<GroupMemberResponse> getMembers(Long groupId) {
    validateGroupExists(groupId);
    return groupMapper.findMembersByGroupId(groupId);
  }

  private void validateGroupExists(Long groupId) {
    if (groupMapper.findActiveById(groupId) == null) {
      throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
    }
  }
}

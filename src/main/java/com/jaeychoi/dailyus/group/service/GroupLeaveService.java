package com.jaeychoi.dailyus.group.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupLeaveResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupLeaveService {

  private final GroupMapper groupMapper;

  @Transactional
  public GroupLeaveResponse leave(Long groupId, Long userId) {
    Group group = getById(groupId);

    validateLeavable(group, userId);
    groupMapper.deleteMember(groupId, userId);
    groupMapper.decreaseMemberCount(groupId);

    return new GroupLeaveResponse(groupId, userId);
  }

  private Group getById(Long groupId) {
    Group group = groupMapper.findActiveById(groupId);
    if (group == null) {
      throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
    }
    return group;
  }

  private void validateLeavable(Group group, Long userId) {
    validateJoined(group.getGroupId(), userId);
    validateNotOwner(group.getOwnerId(), userId);
  }

  private void validateJoined(Long groupId, Long userId) {
    if (!groupMapper.existsMemberByIdAndMemberId(groupId, userId)) {
      throw new BaseException(ErrorCode.GROUP_NOT_JOINED);
    }
  }

  private void validateNotOwner(Long ownerId, Long userId) {
    if (ownerId.equals(userId)) {
      throw new BaseException(ErrorCode.GROUP_OWNER_CANNOT_LEAVE);
    }
  }
}

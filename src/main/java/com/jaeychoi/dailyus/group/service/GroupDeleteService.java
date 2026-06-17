package com.jaeychoi.dailyus.group.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupDeleteService {

  private final GroupMapper groupMapper;

  @Transactional
  public void delete(Long groupId, Long userId) {
    Group group = getById(groupId);

    validateOwner(group.getOwnerId(), userId);
    groupMapper.deleteAllMembers(groupId);
    groupMapper.deleteGroup(groupId);
  }

  private Group getById(Long groupId) {
    Group group = groupMapper.findActiveById(groupId);
    if (group == null) {
      throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
    }
    return group;
  }

  private void validateOwner(Long ownerId, Long userId) {
    if (!ownerId.equals(userId)) {
      throw new BaseException(ErrorCode.FORBIDDEN);
    }
  }
}

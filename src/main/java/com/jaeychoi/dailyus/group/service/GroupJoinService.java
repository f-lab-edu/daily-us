package com.jaeychoi.dailyus.group.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupJoinResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupJoinService {

  private static final int GROUP_MEMBER_LIMIT = 100;
  private static final int USER_GROUP_JOIN_LIMIT = 200;

  private final GroupMapper groupMapper;

  @Transactional
  public GroupJoinResponse join(Long groupId, Long userId) {
    Group group = getById(groupId);

    validateJoinable(group, userId);
    groupMapper.insertMember(groupId, userId);
    groupMapper.increaseMemberCount(groupId);

    return new GroupJoinResponse(groupId, userId);
  }

  private Group getById(Long groupId) {
    Group group = groupMapper.findActiveById(groupId);
    if (group == null) {
      throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
    }
    return group;
  }

  private void validateJoinable(Group group, Long userId) {
    validateNotJoined(group.getGroupId(), userId);
    validateGroupMemberLimit(group.getMemberCount());
    validateUserJoinLimit(userId);
  }

  private void validateNotJoined(Long groupId, Long userId) {
    if (groupMapper.existsMemberByIdAndMemberId(groupId, userId)) {
      throw new BaseException(ErrorCode.GROUP_ALREADY_JOINED);
    }
  }

  private void validateGroupMemberLimit(Integer memberCount) {
    if (memberCount >= GROUP_MEMBER_LIMIT) {
      throw new BaseException(ErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED);
    }
  }

  private void validateUserJoinLimit(Long userId) {
    if (groupMapper.countJoinedGroupsByMemberId(userId) >= USER_GROUP_JOIN_LIMIT) {
      throw new BaseException(ErrorCode.GROUP_USER_JOIN_LIMIT);
    }
  }

}

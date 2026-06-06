package com.jaeychoi.dailyus.group.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupUpdateRequest;
import com.jaeychoi.dailyus.group.dto.GroupUpdateResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupUpdateService {

  private final GroupMapper groupMapper;

  @Transactional
  public GroupUpdateResponse update(Long groupId, Long userId, GroupUpdateRequest request) {
    Group group = groupMapper.findActiveById(groupId);
    if (group == null) {
      throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
    }

    validateOwner(group, userId);
    validateRequest(request);
    applyUpdate(group, request);
    groupMapper.update(group);

    return new GroupUpdateResponse(
        group.getGroupId(),
        group.getName(),
        group.getIntro(),
        group.getGroupImage(),
        group.getOwnerId(),
        group.getMemberCount()
    );
  }

  private void validateOwner(Group group, Long userId) {
    if (!group.getOwnerId().equals(userId)) {
      throw new BaseException(ErrorCode.FORBIDDEN);
    }
  }

  private void validateRequest(GroupUpdateRequest request) {
    if (request.name() == null && request.intro() == null && request.groupImage() == null) {
      throw new BaseException(ErrorCode.INVALID_INPUT);
    }
  }

  private void applyUpdate(Group group, GroupUpdateRequest request) {
    if (request.name() != null) {
      group.setName(request.name());
    }
    if (request.intro() != null) {
      group.setIntro(request.intro());
    }
    if (request.groupImage() != null) {
      group.setGroupImage(request.groupImage());
    }
  }
}

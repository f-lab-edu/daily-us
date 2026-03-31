package com.jaeychoi.dailyus.group.service;

import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupCreateRequest;
import com.jaeychoi.dailyus.group.dto.GroupCreateResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupCreateService {

  private final GroupMapper groupMapper;

  @Transactional
  public GroupCreateResponse create(Long ownerId, GroupCreateRequest request) {
    Group group = Group.builder()
        .name(request.name())
        .intro(request.intro())
        .groupImage(request.groupImage())
        .ownerId(ownerId)
        .build();

    groupMapper.insert(group);
    groupMapper.insertMember(group.getGroupId(), ownerId);

    return new GroupCreateResponse(
        group.getGroupId(),
        group.getName(),
        group.getIntro(),
        group.getGroupImage(),
        group.getOwnerId()
    );
  }
}

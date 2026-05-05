package com.jaeychoi.dailyus.group.mapper;

import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupDetailRow;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper {

  void insert(Group group);

  void insertMember(Long groupId, Long userId);

  void increaseMemberCount(Long groupId);

  Group findActiveById(Long groupId);

  GroupDetailRow findDetailById(Long groupId);

  boolean existsMemberByIdAndMemberId(Long groupId, Long userId);

  int countJoinedGroupsByMemberId(Long userId);
}

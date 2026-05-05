package com.jaeychoi.dailyus.group.mapper;

import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupMemberResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper {

  void insert(Group group);

  void insertMember(Long groupId, Long userId);

  void increaseMemberCount(Long groupId);

  Group findActiveById(Long groupId);

  List<GroupMemberResponse> findMembersByGroupId(Long groupId);

  boolean existsMemberByIdAndMemberId(Long groupId, Long userId);

  int countJoinedGroupsByMemberId(Long userId);
}

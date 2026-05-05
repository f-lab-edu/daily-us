package com.jaeychoi.dailyus.group.mapper;

import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupListRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface GroupMapper {

  void insert(Group group);

  void insertMember(Long groupId, Long userId);

  void increaseMemberCount(Long groupId);

  Group findActiveById(Long groupId);

  boolean existsMemberByIdAndMemberId(Long groupId, Long userId);

  int countJoinedGroupsByMemberId(Long userId);

  List<GroupListRow> findGroupList(
      @Param("size") Long size,
      @Param("createdAt") LocalDateTime createdAt,
      @Param("groupId") Long groupId
  );
}

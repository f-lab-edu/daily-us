package com.jaeychoi.dailyus.group.mapper;

import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupDetailRow;
import com.jaeychoi.dailyus.group.dto.GroupMemberResponse;
import com.jaeychoi.dailyus.group.dto.GroupListRow;
import com.jaeychoi.dailyus.group.dto.GroupMemberRankRow;
import com.jaeychoi.dailyus.user.dto.UserGroupItemResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper {

  void insert(Group group);

  void insertMember(Long groupId, Long userId);

  void increaseMemberCount(Long groupId);

  void deleteMember(Long groupId, Long userId);

  void decreaseMemberCount(Long groupId);

  Group findActiveById(Long groupId);

  List<GroupMemberResponse> findMembersByGroupId(Long groupId);

  GroupDetailRow findDetailById(Long groupId);

  boolean existsMemberByIdAndMemberId(Long groupId, Long userId);

  int countJoinedGroupsByMemberId(Long userId);

  List<GroupMemberRankRow> findMemberRanks(Long groupId, LocalDateTime startAt,
      LocalDateTime endAt);

  List<Long> findMembersByMemberId(Long memberId);

  List<GroupListRow> findGroupList(Long size, LocalDateTime createdAt, Long groupId);

  List<UserGroupItemResponse> findJoinedGroupsByUserId(Long userId);
}

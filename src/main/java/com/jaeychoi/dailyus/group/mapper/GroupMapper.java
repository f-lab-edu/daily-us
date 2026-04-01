package com.jaeychoi.dailyus.group.mapper;

import com.jaeychoi.dailyus.group.domain.Group;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupMapper {

  void insert(Group group);

  void insertMember(Long groupId, Long userId);

  void increaseMemberCount(Long groupId);
}

package com.jaeychoi.dailyus.user.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserFollowMapper {

  boolean existsByFollowerAndFollowee(Long follower, Long followee);

  void insert(Long follower, Long followee);

  int delete(Long follower, Long followee);

  List<Long> findFollowerIdsByFollowee(Long followee);
}

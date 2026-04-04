package com.jaeychoi.dailyus.user.mapper;

import com.jaeychoi.dailyus.user.domain.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {


  boolean existsActiveByEmail(String email);

  boolean existsActiveByNickname(String nickname);

  User findActiveById(Long userId);

  User findActiveByEmail(String email);

  boolean existsActiveById(Long userId);

  void incrementFollowerCount(Long userId);

  void incrementFolloweeCount(Long userId);

  void decrementFollowerCount(Long userId);

  void decrementFolloweeCount(Long userId);

  void insert(User user);
}

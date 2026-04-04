package com.jaeychoi.dailyus.user.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.dto.UserFollowResponse;
import com.jaeychoi.dailyus.user.mapper.UserFollowMapper;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserFollowService {

  private final UserMapper userMapper;
  private final UserFollowMapper userFollowMapper;

  @Transactional
  public UserFollowResponse follow(Long follower, Long followee) {
    validateFollowRequest(follower, followee);

    if (userFollowMapper.existsByFollowerAndFollowee(follower, followee)) {
      throw new BaseException(ErrorCode.FOLLOW_ALREADY_EXISTS);
    }

    userFollowMapper.insert(follower, followee);
    userMapper.incrementFolloweeCount(follower);
    userMapper.incrementFollowerCount(followee);

    User targetUser = getActiveUser(followee);
    return new UserFollowResponse(followee, true, targetUser.getFollowerCount(),
        targetUser.getFolloweeCount());
  }

  @Transactional
  public UserFollowResponse unfollow(Long follower, Long followee) {
    validateTargetUsers(follower, followee);

    int deletedCount = userFollowMapper.delete(follower, followee);
    if (deletedCount == 0) {
      throw new BaseException(ErrorCode.FOLLOW_NOT_FOUND);
    }

    userMapper.decrementFolloweeCount(follower);
    userMapper.decrementFollowerCount(followee);

    User targetUser = getActiveUser(followee);
    return new UserFollowResponse(followee, false, targetUser.getFollowerCount(),
        targetUser.getFolloweeCount());
  }

  private void validateFollowRequest(Long follower, Long followee) {
    if (follower.equals(followee)) {
      throw new BaseException(ErrorCode.SELF_FOLLOW_NOT_ALLOWED);
    }
    validateTargetUsers(follower, followee);
  }

  private void validateTargetUsers(Long follower, Long followee) {
    if (!userMapper.existsActiveById(follower) || !userMapper.existsActiveById(followee)) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }
  }

  private User getActiveUser(Long userId) {
    User user = userMapper.findActiveById(userId);
    if (user == null) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }
    return user;
  }
}

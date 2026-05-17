package com.jaeychoi.dailyus.user.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.dto.UserMyProfileResponse;
import com.jaeychoi.dailyus.user.dto.UserProfileResponse;
import com.jaeychoi.dailyus.user.mapper.UserFollowMapper;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserMapper userMapper;
  private final UserFollowMapper userFollowMapper;
  private final PostMapper postMapper;

  public UserMyProfileResponse getMyProfile(Long userId) {
    User user = userMapper.findActiveById(userId);
    if (user == null) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }

    return new UserMyProfileResponse(
        user.getUserId(),
        user.getEmail(),
        user.getNickname(),
        user.getIntro(),
        user.getProfileImage(),
        user.getFollowerCount(),
        user.getFolloweeCount(),
        postMapper.countActiveByUserId(userId)
    );
  }

  public UserProfileResponse getProfile(Long requesterId, Long targetUserId) {
    if (!userMapper.existsActiveById(requesterId)) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }

    User targetUser = userMapper.findActiveById(targetUserId);
    if (targetUser == null) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }

    boolean following = !requesterId.equals(targetUserId)
        && userFollowMapper.existsByFollowerAndFollowee(requesterId, targetUserId);

    return new UserProfileResponse(
        targetUser.getUserId(),
        targetUser.getNickname(),
        targetUser.getIntro(),
        targetUser.getProfileImage(),
        targetUser.getFollowerCount(),
        targetUser.getFolloweeCount(),
        postMapper.countActiveByUserId(targetUserId),
        following
    );
  }
}

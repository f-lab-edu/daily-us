package com.jaeychoi.dailyus.user.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.dto.UserProfileResponse;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

  private final UserMapper userMapper;
  private final PostMapper postMapper;

  public UserProfileResponse getProfile(Long userId) {
    User user = userMapper.findActiveById(userId);
    if (user == null) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }

    return new UserProfileResponse(
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
}

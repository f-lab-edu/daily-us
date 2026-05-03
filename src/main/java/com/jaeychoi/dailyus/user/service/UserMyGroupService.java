package com.jaeychoi.dailyus.user.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import com.jaeychoi.dailyus.user.dto.UserGroupResponse;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserMyGroupService {

  private final GroupMapper groupMapper;
  private final UserMapper userMapper;

  public UserGroupResponse getMyGroups(Long userId) {
    validateActiveUser(userId);
    return new UserGroupResponse(groupMapper.findJoinedGroupsByUserId(userId));
  }

  private void validateActiveUser(Long userId) {
    if (!userMapper.existsActiveById(userId)) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }
  }
}

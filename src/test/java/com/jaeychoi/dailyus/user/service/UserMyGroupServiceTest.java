package com.jaeychoi.dailyus.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import com.jaeychoi.dailyus.user.dto.UserGroupItemResponse;
import com.jaeychoi.dailyus.user.dto.UserGroupResponse;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserMyGroupServiceTest {

  @Mock
  private GroupMapper groupMapper;

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private UserMyGroupService userMyGroupService;

  @Test
  void getMyGroupsReturnsJoinedGroups() {
    // given
    Long userId = 1L;
    List<UserGroupItemResponse> groups = List.of(
        new UserGroupItemResponse(10L, "daily-us", "https://example.com/group.png"),
        new UserGroupItemResponse(11L, "backend-study", null)
    );
    when(userMapper.existsActiveById(userId)).thenReturn(true);
    when(groupMapper.findJoinedGroupsByUserId(userId)).thenReturn(groups);

    // when
    UserGroupResponse response = userMyGroupService.getMyGroups(userId);

    // then
    assertThat(response.items()).containsExactlyElementsOf(groups);
  }

  @Test
  void getMyGroupsThrowsWhenUserDoesNotExist() {
    // given
    Long userId = 1L;
    when(userMapper.existsActiveById(userId)).thenReturn(false);

    // when
    // then
    assertThatThrownBy(() -> userMyGroupService.getMyGroups(userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);

    verify(groupMapper, never()).findJoinedGroupsByUserId(userId);
  }
}

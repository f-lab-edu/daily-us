package com.jaeychoi.dailyus.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupMemberResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupMembersServiceTest {

  @Mock
  private GroupMapper groupMapper;

  @InjectMocks
  private GroupMembersService groupMembersService;

  @Test
  void getMembersReturnsGroupMembers() {
    when(groupMapper.findActiveById(10L)).thenReturn(Group.builder().groupId(10L).build());
    when(groupMapper.findMembersByGroupId(10L)).thenReturn(List.of(
        new GroupMemberResponse(1L, "user-1", "https://example.com/1.png"),
        new GroupMemberResponse(2L, "user-2", null)
    ));

    List<GroupMemberResponse> response = groupMembersService.getMembers(10L);

    assertThat(response).hasSize(2);
    assertThat(response.get(0).userId()).isEqualTo(1L);
    assertThat(response.get(1).nickname()).isEqualTo("user-2");
  }

  @Test
  void getMembersThrowsWhenGroupDoesNotExist() {
    when(groupMapper.findActiveById(10L)).thenReturn(null);

    assertThatThrownBy(() -> groupMembersService.getMembers(10L))
        .isInstanceOfSatisfying(BaseException.class,
            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.GROUP_NOT_FOUND));
  }
}

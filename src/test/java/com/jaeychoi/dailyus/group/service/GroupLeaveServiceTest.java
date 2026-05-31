package com.jaeychoi.dailyus.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupLeaveResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupLeaveServiceTest {

  @Mock
  private GroupMapper groupMapper;

  @InjectMocks
  private GroupLeaveService groupLeaveService;

  @Test
  void leaveDeletesMembershipAndDecreasesMemberCount() {
    Long groupId = 10L;
    Long userId = 20L;
    Group group = Group.builder()
        .groupId(groupId)
        .ownerId(30L)
        .memberCount(2)
        .build();
    when(groupMapper.findActiveById(groupId)).thenReturn(group);
    when(groupMapper.existsMemberByIdAndMemberId(groupId, userId)).thenReturn(true);

    GroupLeaveResponse response = groupLeaveService.leave(groupId, userId);

    verify(groupMapper).deleteMember(groupId, userId);
    verify(groupMapper).decreaseMemberCount(groupId);
    assertThat(response.groupId()).isEqualTo(groupId);
    assertThat(response.userId()).isEqualTo(userId);
  }

  @Test
  void leaveThrowsWhenGroupDoesNotExist() {
    Long groupId = 10L;
    Long userId = 20L;
    when(groupMapper.findActiveById(groupId)).thenReturn(null);

    assertThatThrownBy(() -> groupLeaveService.leave(groupId, userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GROUP_NOT_FOUND);

    verify(groupMapper, never()).deleteMember(groupId, userId);
    verify(groupMapper, never()).decreaseMemberCount(groupId);
  }

  @Test
  void leaveThrowsWhenUserIsNotMember() {
    Long groupId = 10L;
    Long userId = 20L;
    Group group = Group.builder()
        .groupId(groupId)
        .ownerId(30L)
        .memberCount(2)
        .build();
    when(groupMapper.findActiveById(groupId)).thenReturn(group);
    when(groupMapper.existsMemberByIdAndMemberId(groupId, userId)).thenReturn(false);

    assertThatThrownBy(() -> groupLeaveService.leave(groupId, userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GROUP_NOT_JOINED);

    verify(groupMapper, never()).deleteMember(groupId, userId);
    verify(groupMapper, never()).decreaseMemberCount(groupId);
  }

  @Test
  void leaveThrowsWhenUserIsOwner() {
    Long groupId = 10L;
    Long userId = 20L;
    Group group = Group.builder()
        .groupId(groupId)
        .ownerId(userId)
        .memberCount(2)
        .build();
    when(groupMapper.findActiveById(groupId)).thenReturn(group);
    when(groupMapper.existsMemberByIdAndMemberId(groupId, userId)).thenReturn(true);

    assertThatThrownBy(() -> groupLeaveService.leave(groupId, userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GROUP_OWNER_CANNOT_LEAVE);

    verify(groupMapper, never()).deleteMember(groupId, userId);
    verify(groupMapper, never()).decreaseMemberCount(groupId);
  }
}

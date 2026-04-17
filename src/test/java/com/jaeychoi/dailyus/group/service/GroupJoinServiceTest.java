package com.jaeychoi.dailyus.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupJoinResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupJoinServiceTest {

  @Mock
  private GroupMapper groupMapper;

  @InjectMocks
  private GroupJoinService groupJoinService;

  @Test
  void joinPersistsMembershipAndIncreasesMemberCount() {
    // given
    Long groupId = 10L;
    Long userId = 20L;
    Group group = Group.builder()
        .groupId(groupId)
        .memberCount(99)
        .build();
    when(groupMapper.findActiveById(groupId)).thenReturn(group);
    when(groupMapper.existsMemberByIdAndMemberId(groupId, userId)).thenReturn(false);
    when(groupMapper.countJoinedGroupsByMemberId(userId)).thenReturn(3);

    // when
    GroupJoinResponse response = groupJoinService.join(groupId, userId);

    // then
    verify(groupMapper).insertMember(groupId, userId);
    verify(groupMapper).increaseMemberCount(groupId);
    assertThat(response.groupId()).isEqualTo(groupId);
    assertThat(response.userId()).isEqualTo(userId);
  }

  @Test
  void joinThrowsWhenGroupDoesNotExist() {
    // given
    Long groupId = 10L;
    Long userId = 20L;
    when(groupMapper.findActiveById(groupId)).thenReturn(null);

    // when
    // then
    assertThatThrownBy(() -> groupJoinService.join(groupId, userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GROUP_NOT_FOUND);

    verify(groupMapper, never()).insertMember(groupId, userId);
    verify(groupMapper, never()).increaseMemberCount(groupId);
  }

  @Test
  void joinThrowsWhenUserAlreadyJoined() {
    // given
    Long groupId = 10L;
    Long userId = 20L;
    Group group = Group.builder()
        .groupId(groupId)
        .memberCount(10)
        .build();
    when(groupMapper.findActiveById(groupId)).thenReturn(group);
    when(groupMapper.existsMemberByIdAndMemberId(groupId, userId)).thenReturn(true);

    // when
    // then
    assertThatThrownBy(() -> groupJoinService.join(groupId, userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GROUP_ALREADY_JOINED);

    verify(groupMapper, never()).insertMember(groupId, userId);
    verify(groupMapper, never()).increaseMemberCount(groupId);
  }

  @Test
  void joinThrowsWhenGroupMemberLimitExceeded() {
    // given
    Long groupId = 10L;
    Long userId = 20L;
    Group group = Group.builder()
        .groupId(groupId)
        .memberCount(100)
        .build();
    when(groupMapper.findActiveById(groupId)).thenReturn(group);
    when(groupMapper.existsMemberByIdAndMemberId(groupId, userId)).thenReturn(false);

    // when
    // then
    assertThatThrownBy(() -> groupJoinService.join(groupId, userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED);

    verify(groupMapper, never()).insertMember(groupId, userId);
    verify(groupMapper, never()).increaseMemberCount(groupId);
  }

  @Test
  void joinThrowsWhenUserJoinLimitExceeded() {
    // given
    Long groupId = 10L;
    Long userId = 20L;
    Group group = Group.builder()
        .groupId(groupId)
        .memberCount(99)
        .build();
    when(groupMapper.findActiveById(groupId)).thenReturn(group);
    when(groupMapper.existsMemberByIdAndMemberId(groupId, userId)).thenReturn(false);
    when(groupMapper.countJoinedGroupsByMemberId(userId)).thenReturn(200);

    // when
    // then
    assertThatThrownBy(() -> groupJoinService.join(groupId, userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GROUP_USER_JOIN_LIMIT);

    verify(groupMapper, never()).insertMember(groupId, userId);
    verify(groupMapper, never()).increaseMemberCount(groupId);
  }
}

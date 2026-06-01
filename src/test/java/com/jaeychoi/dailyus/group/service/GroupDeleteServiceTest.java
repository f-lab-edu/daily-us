package com.jaeychoi.dailyus.group.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupDeleteServiceTest {

  @Mock
  private GroupMapper groupMapper;

  @InjectMocks
  private GroupDeleteService groupDeleteService;

  @Test
  void deleteSoftDeletesGroupAndDeletesMembers() {
    Long groupId = 10L;
    Long ownerId = 20L;
    Group group = Group.builder()
        .groupId(groupId)
        .ownerId(ownerId)
        .build();
    when(groupMapper.findActiveById(groupId)).thenReturn(group);

    groupDeleteService.delete(groupId, ownerId);

    verify(groupMapper).deleteAllMembers(groupId);
    verify(groupMapper).deleteGroup(groupId);
  }

  @Test
  void deleteThrowsWhenGroupDoesNotExist() {
    Long groupId = 10L;
    Long ownerId = 20L;
    when(groupMapper.findActiveById(groupId)).thenReturn(null);

    assertThatThrownBy(() -> groupDeleteService.delete(groupId, ownerId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GROUP_NOT_FOUND);

    verify(groupMapper, never()).deleteAllMembers(groupId);
    verify(groupMapper, never()).deleteGroup(groupId);
  }

  @Test
  void deleteThrowsWhenUserIsNotOwner() {
    Long groupId = 10L;
    Long ownerId = 20L;
    Long userId = 30L;
    Group group = Group.builder()
        .groupId(groupId)
        .ownerId(ownerId)
        .build();
    when(groupMapper.findActiveById(groupId)).thenReturn(group);

    assertThatThrownBy(() -> groupDeleteService.delete(groupId, userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);

    verify(groupMapper, never()).deleteAllMembers(groupId);
    verify(groupMapper, never()).deleteGroup(groupId);
  }
}

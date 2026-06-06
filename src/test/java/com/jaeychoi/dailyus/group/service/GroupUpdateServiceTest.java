package com.jaeychoi.dailyus.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupUpdateRequest;
import com.jaeychoi.dailyus.group.dto.GroupUpdateResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupUpdateServiceTest {

  @Mock
  private GroupMapper groupMapper;

  @InjectMocks
  private GroupUpdateService groupUpdateService;

  @Test
  void updateReturnsUpdatedGroupWhenOwnerMatches() {
    Long ownerId = 2L;
    Group group = Group.builder()
        .groupId(1L)
        .name("before")
        .intro("before intro")
        .groupImage("before.png")
        .ownerId(ownerId)
        .memberCount(3)
        .build();
    when(groupMapper.findActiveById(1L)).thenReturn(group);

    GroupUpdateResponse response = groupUpdateService.update(
        1L,
        ownerId,
        new GroupUpdateRequest("after", "after intro", "after.png")
    );

    assertThat(response.groupId()).isEqualTo(1L);
    assertThat(response.name()).isEqualTo("after");
    assertThat(response.intro()).isEqualTo("after intro");
    assertThat(response.groupImage()).isEqualTo("after.png");
    assertThat(response.ownerId()).isEqualTo(ownerId);
    assertThat(response.memberCount()).isEqualTo(3);
    verify(groupMapper).update(group);
  }

  @Test
  void updateReturnsUpdatedGroupWhenOnlyIntroChanges() {
    Long ownerId = 2L;
    Group group = Group.builder()
        .groupId(1L)
        .name("before")
        .intro("before intro")
        .groupImage("before.png")
        .ownerId(ownerId)
        .memberCount(3)
        .build();
    when(groupMapper.findActiveById(1L)).thenReturn(group);

    GroupUpdateResponse response = groupUpdateService.update(
        1L,
        ownerId,
        new GroupUpdateRequest(null, "after intro", null)
    );

    assertThat(response.name()).isEqualTo("before");
    assertThat(response.intro()).isEqualTo("after intro");
    assertThat(response.groupImage()).isEqualTo("before.png");
    verify(groupMapper).update(group);
  }

  @Test
  void updateThrowsWhenGroupDoesNotExist() {
    when(groupMapper.findActiveById(1L)).thenReturn(null);

    assertThatThrownBy(() -> groupUpdateService.update(
        1L,
        2L,
        new GroupUpdateRequest("after", "after intro", "after.png")
    ))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GROUP_NOT_FOUND);
  }

  @Test
  void updateThrowsWhenUserIsNotOwner() {
    Group group = Group.builder()
        .groupId(1L)
        .ownerId(99L)
        .build();
    when(groupMapper.findActiveById(1L)).thenReturn(group);

    assertThatThrownBy(() -> groupUpdateService.update(
        1L,
        2L,
        new GroupUpdateRequest("after", "after intro", "after.png")
    ))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  void updateThrowsWhenNoFieldIsProvided() {
    Group group = Group.builder()
        .groupId(1L)
        .ownerId(2L)
        .name("before")
        .intro("before intro")
        .groupImage("before.png")
        .memberCount(3)
        .build();
    when(groupMapper.findActiveById(1L)).thenReturn(group);

    assertThatThrownBy(() -> groupUpdateService.update(
        1L,
        2L,
        new GroupUpdateRequest(null, null, null)
    ))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_INPUT);
  }
}

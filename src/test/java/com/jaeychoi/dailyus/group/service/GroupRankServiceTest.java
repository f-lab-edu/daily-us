package com.jaeychoi.dailyus.group.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.group.domain.Group;
import com.jaeychoi.dailyus.group.dto.GroupMemberRankRow;
import com.jaeychoi.dailyus.group.dto.GroupRankResponse;
import com.jaeychoi.dailyus.group.mapper.GroupMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GroupRankServiceTest {

  @Mock
  private GroupMapper groupMapper;

  @InjectMocks
  private GroupRankService groupRankService;

  @Test
  void getRankReturnsMonthlyPostRanking() {
    Long groupId = 10L;
    Long userId = 2L;
    when(groupMapper.findActiveById(groupId)).thenReturn(Group.builder().groupId(groupId).build());
    when(groupMapper.existsMemberByIdAndMemberId(groupId, userId)).thenReturn(true);
    when(groupMapper.findMemberRanks(eq(groupId), any(), any())).thenReturn(List.of(
        new GroupMemberRankRow(1, 2L, "bravo", null, 5L),
        new GroupMemberRankRow(1, 3L, "charlie", "https://example.com/charlie.png", 5L),
        new GroupMemberRankRow(2, 1L, "alpha", "https://example.com/alpha.png", 2L)
    ));

    GroupRankResponse response = groupRankService.getRank(groupId, userId);

    assertThat(response.groupId()).isEqualTo(groupId);
    assertThat(response.rank()).hasSize(3);
    assertThat(response.rank().get(0).rank()).isEqualTo(1);
    assertThat(response.rank().get(0).userId()).isEqualTo(2L);
    assertThat(response.rank().get(0).postCount()).isEqualTo(5L);
    assertThat(response.rank().get(1).rank()).isEqualTo(1);
    assertThat(response.rank().get(1).userId()).isEqualTo(3L);
    assertThat(response.rank().get(2).rank()).isEqualTo(2);
    assertThat(response.rank().get(2).userId()).isEqualTo(1L);
    assertThat(response.rank().get(2).postCount()).isEqualTo(2L);
  }

  @Test
  void getRankThrowsWhenGroupDoesNotExist() {
    Long groupId = 10L;
    Long userId = 2L;
    when(groupMapper.findActiveById(groupId)).thenReturn(null);

    assertThatThrownBy(() -> groupRankService.getRank(groupId, userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.GROUP_NOT_FOUND);

    verify(groupMapper, never()).existsMemberByIdAndMemberId(groupId, userId);
    verify(groupMapper, never()).findMemberRanks(eq(groupId), any(), any());
  }

  @Test
  void getRankThrowsWhenUserIsNotGroupMember() {
    Long groupId = 10L;
    Long userId = 2L;
    when(groupMapper.findActiveById(groupId)).thenReturn(Group.builder().groupId(groupId).build());
    when(groupMapper.existsMemberByIdAndMemberId(groupId, userId)).thenReturn(false);

    assertThatThrownBy(() -> groupRankService.getRank(groupId, userId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FORBIDDEN);

    verify(groupMapper, never()).findMemberRanks(eq(groupId), any(), any());
  }
}

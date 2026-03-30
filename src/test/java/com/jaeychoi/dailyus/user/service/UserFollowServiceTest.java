package com.jaeychoi.dailyus.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.dto.UserFollowResponse;
import com.jaeychoi.dailyus.user.mapper.UserFollowMapper;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserFollowServiceTest {

  @Mock
  private UserMapper userMapper;

  @Mock
  private UserFollowMapper userFollowMapper;

  @InjectMocks
  private UserFollowService userFollowService;

  @Test
  void followCreatesRelationshipAndUpdatesCounts() {
    // given
    Long loginUserId = 1L;
    Long targetUserId = 2L;
    User targetUser = User.builder()
        .userId(targetUserId)
        .followerCount(3L)
        .followeeCount(1L)
        .build();

    when(userMapper.existsActiveById(loginUserId)).thenReturn(true);
    when(userMapper.existsActiveById(targetUserId)).thenReturn(true);
    when(userFollowMapper.existsByFollowerAndFollowee(loginUserId, targetUserId)).thenReturn(false);
    when(userMapper.findActiveById(targetUserId)).thenReturn(targetUser);

    // when
    UserFollowResponse response = userFollowService.follow(loginUserId, targetUserId);

    // then
    verify(userFollowMapper).insert(loginUserId, targetUserId);
    verify(userMapper).incrementFolloweeCount(loginUserId);
    verify(userMapper).incrementFollowerCount(targetUserId);
    assertThat(response.followee()).isEqualTo(targetUserId);
    assertThat(response.following()).isTrue();
    assertThat(response.followerCount()).isEqualTo(3L);
    assertThat(response.followeeCount()).isEqualTo(1L);
  }

  @Test
  void followThrowsWhenTargetIsSelf() {
    // given
    Long loginUserId = 1L;

    // when
    // then
    assertThatThrownBy(() -> userFollowService.follow(loginUserId, loginUserId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.SELF_FOLLOW_NOT_ALLOWED);

    verify(userMapper, never()).existsActiveById(loginUserId);
    verify(userFollowMapper, never()).insert(loginUserId, loginUserId);
  }

  @Test
  void followThrowsWhenRelationshipAlreadyExists() {
    // given
    Long loginUserId = 1L;
    Long targetUserId = 2L;
    when(userMapper.existsActiveById(loginUserId)).thenReturn(true);
    when(userMapper.existsActiveById(targetUserId)).thenReturn(true);
    when(userFollowMapper.existsByFollowerAndFollowee(loginUserId, targetUserId)).thenReturn(true);

    // when
    // then
    assertThatThrownBy(() -> userFollowService.follow(loginUserId, targetUserId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FOLLOW_ALREADY_EXISTS);

    verify(userFollowMapper, never()).insert(loginUserId, targetUserId);
  }

  @Test
  void unfollowDeletesRelationshipAndUpdatesCounts() {
    // given
    Long loginUserId = 1L;
    Long targetUserId = 2L;
    User targetUser = User.builder()
        .userId(targetUserId)
        .followerCount(2L)
        .followeeCount(1L)
        .build();

    when(userMapper.existsActiveById(loginUserId)).thenReturn(true);
    when(userMapper.existsActiveById(targetUserId)).thenReturn(true);
    when(userFollowMapper.delete(loginUserId, targetUserId)).thenReturn(1);
    when(userMapper.findActiveById(targetUserId)).thenReturn(targetUser);

    // when
    UserFollowResponse response = userFollowService.unfollow(loginUserId, targetUserId);

    // then
    verify(userMapper).decrementFolloweeCount(loginUserId);
    verify(userMapper).decrementFollowerCount(targetUserId);
    assertThat(response.followee()).isEqualTo(targetUserId);
    assertThat(response.following()).isFalse();
    assertThat(response.followerCount()).isEqualTo(2L);
    assertThat(response.followeeCount()).isEqualTo(1L);
  }

  @Test
  void unfollowThrowsWhenRelationshipDoesNotExist() {
    // given
    Long loginUserId = 1L;
    Long targetUserId = 2L;
    when(userMapper.existsActiveById(loginUserId)).thenReturn(true);
    when(userMapper.existsActiveById(targetUserId)).thenReturn(true);
    when(userFollowMapper.delete(loginUserId, targetUserId)).thenReturn(0);

    // when
    // then
    assertThatThrownBy(() -> userFollowService.unfollow(loginUserId, targetUserId))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.FOLLOW_NOT_FOUND);

    verify(userMapper, never()).decrementFolloweeCount(loginUserId);
    verify(userMapper, never()).decrementFollowerCount(targetUserId);
  }
}

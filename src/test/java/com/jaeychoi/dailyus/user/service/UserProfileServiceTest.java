package com.jaeychoi.dailyus.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.dto.UserMyProfileResponse;
import com.jaeychoi.dailyus.user.dto.UserProfileResponse;
import com.jaeychoi.dailyus.user.dto.UserProfileUpdateRequest;
import com.jaeychoi.dailyus.user.mapper.UserFollowMapper;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

  @Mock
  private UserMapper userMapper;

  @Mock
  private UserFollowMapper userFollowMapper;

  @Mock
  private PostMapper postMapper;

  @InjectMocks
  private UserProfileService userProfileService;

  @Test
  void getMyProfileReturnsUserProfile() {
    User user = User.builder()
        .userId(1L)
        .email("user@example.com")
        .nickname("dailyus")
        .intro("sharing daily workouts")
        .profileImage("https://cdn.example.com/profile.png")
        .followerCount(3L)
        .followeeCount(7L)
        .build();
    when(userMapper.findActiveById(1L)).thenReturn(user);
    when(postMapper.countActiveByUserId(1L)).thenReturn(5L);

    UserMyProfileResponse response = userProfileService.getMyProfile(1L);

    assertThat(response.userId()).isEqualTo(1L);
    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.nickname()).isEqualTo("dailyus");
    assertThat(response.intro()).isEqualTo("sharing daily workouts");
    assertThat(response.profileImage()).isEqualTo("https://cdn.example.com/profile.png");
    assertThat(response.followerCount()).isEqualTo(3L);
    assertThat(response.followeeCount()).isEqualTo(7L);
    assertThat(response.postCount()).isEqualTo(5L);
  }

  @Test
  void getMyProfileThrowsWhenUserDoesNotExist() {
    when(userMapper.findActiveById(1L)).thenReturn(null);

    assertThatThrownBy(() -> userProfileService.getMyProfile(1L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  void getProfileReturnsTargetUserProfile() {
    User targetUser = User.builder()
        .userId(2L)
        .email("target@example.com")
        .nickname("target")
        .intro("intro")
        .profileImage("https://cdn.example.com/target.png")
        .followerCount(11L)
        .followeeCount(4L)
        .build();
    when(userMapper.existsActiveById(1L)).thenReturn(true);
    when(userMapper.findActiveById(2L)).thenReturn(targetUser);
    when(userFollowMapper.existsByFollowerAndFollowee(1L, 2L)).thenReturn(true);
    when(postMapper.countActiveByUserId(2L)).thenReturn(9L);

    UserProfileResponse response = userProfileService.getProfile(1L, 2L);

    assertThat(response.userId()).isEqualTo(2L);
    assertThat(response.nickname()).isEqualTo("target");
    assertThat(response.intro()).isEqualTo("intro");
    assertThat(response.profileImage()).isEqualTo("https://cdn.example.com/target.png");
    assertThat(response.followerCount()).isEqualTo(11L);
    assertThat(response.followeeCount()).isEqualTo(4L);
    assertThat(response.postCount()).isEqualTo(9L);
    assertThat(response.following()).isTrue();
  }

  @Test
  void getProfileReturnsNotFollowingWhenRequesterMatchesTarget() {
    User targetUser = User.builder()
        .userId(1L)
        .nickname("dailyus")
        .followerCount(3L)
        .followeeCount(7L)
        .build();
    when(userMapper.existsActiveById(1L)).thenReturn(true);
    when(userMapper.findActiveById(1L)).thenReturn(targetUser);
    when(postMapper.countActiveByUserId(1L)).thenReturn(5L);

    UserProfileResponse response = userProfileService.getProfile(1L, 1L);

    assertThat(response.following()).isFalse();
  }

  @Test
  void getProfileThrowsWhenRequesterDoesNotExist() {
    when(userMapper.existsActiveById(1L)).thenReturn(false);

    assertThatThrownBy(() -> userProfileService.getProfile(1L, 2L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  void getProfileThrowsWhenTargetUserDoesNotExist() {
    when(userMapper.existsActiveById(1L)).thenReturn(true);
    when(userMapper.findActiveById(2L)).thenReturn(null);

    assertThatThrownBy(() -> userProfileService.getProfile(1L, 2L))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  void updateProfileUpdatesOnlyRequestedFields() {
    User currentUser = User.builder()
        .userId(1L)
        .email("user@example.com")
        .nickname("dailyus")
        .intro("current intro")
        .profileImage("https://cdn.example.com/original.png")
        .followerCount(3L)
        .followeeCount(7L)
        .build();
    User updatedUser = User.builder()
        .userId(1L)
        .email("user@example.com")
        .nickname("dailyus-new")
        .intro("current intro")
        .profileImage("https://cdn.example.com/updated.png")
        .followerCount(3L)
        .followeeCount(7L)
        .build();
    UserProfileUpdateRequest request = new UserProfileUpdateRequest(
        "dailyus-new",
        null,
        "https://cdn.example.com/updated.png"
    );

    when(userMapper.findActiveById(1L)).thenReturn(currentUser, updatedUser);
    when(userMapper.existsActiveByNickname("dailyus-new")).thenReturn(false);
    when(postMapper.countActiveByUserId(1L)).thenReturn(5L);

    UserProfileResponse response = userProfileService.updateProfile(1L, request);

    verify(userMapper).updateProfile(currentUser);
    assertThat(currentUser.getNickname()).isEqualTo("dailyus-new");
    assertThat(currentUser.getIntro()).isEqualTo("current intro");
    assertThat(currentUser.getProfileImage()).isEqualTo("https://cdn.example.com/updated.png");
    assertThat(response.nickname()).isEqualTo("dailyus-new");
    assertThat(response.intro()).isEqualTo("current intro");
    assertThat(response.profileImage()).isEqualTo("https://cdn.example.com/updated.png");
  }

  @Test
  void updateProfileSkipsDuplicateCheckWhenNicknameIsUnchanged() {
    User currentUser = User.builder()
        .userId(1L)
        .email("user@example.com")
        .nickname("dailyus")
        .intro("current intro")
        .profileImage("https://cdn.example.com/original.png")
        .followerCount(3L)
        .followeeCount(7L)
        .build();
    UserProfileUpdateRequest request = new UserProfileUpdateRequest("dailyus", "", "");

    when(userMapper.findActiveById(1L)).thenReturn(currentUser, currentUser);
    when(postMapper.countActiveByUserId(1L)).thenReturn(5L);

    userProfileService.updateProfile(1L, request);

    verify(userMapper, never()).existsActiveByNickname("dailyus");
    verify(userMapper).updateProfile(currentUser);
    assertThat(currentUser.getIntro()).isEmpty();
    assertThat(currentUser.getProfileImage()).isEmpty();
  }

  @Test
  void updateProfileThrowsWhenNicknameAlreadyExists() {
    User currentUser = User.builder()
        .userId(1L)
        .email("user@example.com")
        .nickname("dailyus")
        .build();
    UserProfileUpdateRequest request = new UserProfileUpdateRequest("taken", null, null);

    when(userMapper.findActiveById(1L)).thenReturn(currentUser);
    when(userMapper.existsActiveByNickname("taken")).thenReturn(true);

    assertThatThrownBy(() -> userProfileService.updateProfile(1L, request))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.NICKNAME_ALREADY_EXISTS);

    verify(userMapper, never()).updateProfile(currentUser);
  }
}

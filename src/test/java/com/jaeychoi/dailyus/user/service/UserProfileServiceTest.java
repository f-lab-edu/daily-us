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
import com.jaeychoi.dailyus.user.dto.UserProfileResponse;
import com.jaeychoi.dailyus.user.dto.UserProfileUpdateRequest;
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
  private PostMapper postMapper;

  @InjectMocks
  private UserProfileService userProfileService;

  @Test
  void getProfileReturnsUserProfile() {
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

    UserProfileResponse response = userProfileService.getProfile(1L);

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
  void getProfileThrowsWhenUserDoesNotExist() {
    when(userMapper.findActiveById(1L)).thenReturn(null);

    assertThatThrownBy(() -> userProfileService.getProfile(1L))
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

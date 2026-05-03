package com.jaeychoi.dailyus.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.dto.UserProfileResponse;
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
        .intro("운동 기록 중입니다.")
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
    assertThat(response.intro()).isEqualTo("운동 기록 중입니다.");
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
}

package com.jaeychoi.dailyus.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.user.dto.UserActivityResponse;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

  @Mock
  private UserMapper userMapper;

  @Mock
  private PostMapper postMapper;

  @InjectMocks
  private UserActivityService userActivityService;

  @Test
  void getMyActivitiesReturnsMonthlyActivityDays() {
    when(userMapper.existsActiveById(1L)).thenReturn(true);
    when(postMapper.findActivityDaysByUserId(
        1L,
        LocalDateTime.of(2026, 3, 1, 0, 0),
        LocalDateTime.of(2026, 4, 1, 0, 0)
    )).thenReturn(List.of(10, 11, 12));

    UserActivityResponse response = userActivityService.getMyActivities(1L, 2026, 3);

    assertThat(response.year()).isEqualTo(2026);
    assertThat(response.month()).isEqualTo(3);
    assertThat(response.activityDays()).containsExactly(10, 11, 12);
  }

  @Test
  void getMyActivitiesThrowsWhenUserDoesNotExist() {
    when(userMapper.existsActiveById(1L)).thenReturn(false);

    assertThatThrownBy(() -> userActivityService.getMyActivities(1L, 2026, 3))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  void getMyActivitiesThrowsWhenMonthIsInvalid() {
    when(userMapper.existsActiveById(1L)).thenReturn(true);

    assertThatThrownBy(() -> userActivityService.getMyActivities(1L, 2026, 13))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_ACTIVITY_PERIOD);

    verify(userMapper).existsActiveById(1L);
  }

  @Test
  void getMyActivitiesThrowsWhenTargetMonthIsFuture() {
    when(userMapper.existsActiveById(1L)).thenReturn(true);

    assertThatThrownBy(() -> userActivityService.getMyActivities(1L, 9999, 1))
        .isInstanceOf(BaseException.class)
        .extracting(exception -> ((BaseException) exception).getErrorCode())
        .isEqualTo(ErrorCode.INVALID_ACTIVITY_PERIOD);
  }
}

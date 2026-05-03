package com.jaeychoi.dailyus.user.service;

import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.post.mapper.PostMapper;
import com.jaeychoi.dailyus.user.dto.UserActivityResponse;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserActivityService {

  private final UserMapper userMapper;
  private final PostMapper postMapper;

  public UserActivityResponse getMyActivities(Long userId, Integer year, Integer month) {
    validateActiveUser(userId);

    YearMonth targetMonth = resolveTargetMonth(year, month);
    LocalDateTime startAt = targetMonth.atDay(1).atStartOfDay();
    LocalDateTime endAt = targetMonth.plusMonths(1).atDay(1).atStartOfDay();

    return new UserActivityResponse(
        targetMonth.getYear(),
        targetMonth.getMonthValue(),
        postMapper.findActivityDaysByUserId(userId, startAt, endAt)
    );
  }

  private void validateActiveUser(Long userId) {
    if (!userMapper.existsActiveById(userId)) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }
  }

  private YearMonth resolveTargetMonth(Integer year, Integer month) {
    if (year == null && month == null) {
      return YearMonth.now();
    }

    if (year == null || month == null || month < 1 || month > 12) {
      throw new BaseException(ErrorCode.INVALID_ACTIVITY_PERIOD);
    }

    YearMonth targetMonth = YearMonth.of(year, month);
    if (targetMonth.isAfter(YearMonth.now())) {
      throw new BaseException(ErrorCode.INVALID_ACTIVITY_PERIOD);
    }
    return targetMonth;
  }
}

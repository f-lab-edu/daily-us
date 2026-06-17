package com.jaeychoi.dailyus.user.service;

import com.jaeychoi.dailyus.auth.repository.RefreshTokenRepository;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.user.domain.User;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserWithdrawService {

  private final UserMapper userMapper;
  private final RefreshTokenRepository refreshTokenRepository;

  @Transactional
  public void withdraw(Long userId) {
    User user = userMapper.findActiveById(userId);
    if (user == null) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }

    LocalDateTime withdrawnAt = LocalDateTime.now();
    user.setEmail(buildWithdrawnEmail(user));
    user.setNickname(buildWithdrawnNickname(user));
    user.setDeletedAt(withdrawnAt);

    int deletedCount = userMapper.withdraw(user);
    if (deletedCount == 0) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }

    refreshTokenRepository.delete(userId);
  }

  private String buildWithdrawnEmail(User user) {
    String prefix = "withdrawn+" + user.getUserId() + "+";
    return prefix + truncate(user.getEmail(), 255 - prefix.length());
  }

  private String buildWithdrawnNickname(User user) {
    String prefix = "withdrawn-" + user.getUserId() + "-";
    return prefix + truncate(user.getNickname(), 100 - prefix.length());
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, Math.max(maxLength, 0));
  }
}

package com.jaeychoi.dailyus.user.service;

import com.jaeychoi.dailyus.auth.repository.RefreshTokenRepository;
import com.jaeychoi.dailyus.common.exception.BaseException;
import com.jaeychoi.dailyus.common.exception.ErrorCode;
import com.jaeychoi.dailyus.user.mapper.UserMapper;
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
    if (!userMapper.existsActiveById(userId)) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }

    int deletedCount = userMapper.delete(userId);
    if (deletedCount == 0) {
      throw new BaseException(ErrorCode.USER_NOT_FOUND);
    }

    refreshTokenRepository.delete(userId);
  }
}
